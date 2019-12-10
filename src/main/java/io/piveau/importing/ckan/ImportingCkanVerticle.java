package io.piveau.importing.ckan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.piveau.importing.ckan.response.CkanArrayResult;
import io.piveau.importing.ckan.response.CkanError;
import io.piveau.importing.ckan.response.CkanResponse;
import io.piveau.importing.ckan.response.CkanObjectResult;
import io.piveau.pipe.connector.PipeContext;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.TimeoutStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ImportingCkanVerticle extends AbstractVerticle {

    public static final String ADDRESS = "io.piveau.pipe.importing.ckan.queue";

    private WebClient client;

    private int defaultDelay;

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer(ADDRESS, this::handlePipe);
        client = WebClient.create(vertx);
        ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray().add("PIVEAU_IMPORTING_SEND_LIST_DELAY")));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(envStoreOptions));
        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                defaultDelay = ar.result().getInteger("PIVEAU_IMPORTING_SEND_LIST_DELAY", 8000);
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
        retriever.listen(change -> {
            defaultDelay = change.getNewConfiguration().getInteger("PIVEAU_IMPORTING_SEND_LIST_DELAY", 8000);
        });
    }

    private void handlePipe(Message<PipeContext> message) {
        PipeContext pipeContext = message.body();
        pipeContext.log().info("Import started");

        fetchPage(0, pipeContext, new ArrayList<>());
    }

    private void fetchPage(int offset, PipeContext pipeContext, List<String> identifiers) {
        JsonNode config = pipeContext.getConfig();
        int pageSize = config.path("pageSize").asInt(100);

        String dialect = config.path("dialect").asText("ckan");
        String address = config.path("address").textValue();
        address += dialect.equals("dkan") ? "/api/3/action/current_package_list_with_resources" : "/api/3/action/package_search";

        HttpRequest<Buffer> request = client.getAbs(address);
        if ("dkan".equals(dialect)) {
            request.addQueryParam("limit", String.valueOf(pageSize)).addQueryParam("offset", String.valueOf(offset));
        } else {
            request.addQueryParam("rows", String.valueOf(pageSize)).addQueryParam("start", String.valueOf(offset));
        }

        JsonNode filters = config.path("filters");
        if (!filters.isMissingNode()) {
            filters.fields().forEachRemaining(entry -> request.addQueryParam(entry.getKey(), entry.getValue().asText()));
        }

        if (config.path("incremental").asBoolean(false)) {
            Date lastRun = pipeContext.getPipe().getHeader().getLastRun();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            request.addQueryParam("q", "metadata_modified:[" + dateFormat.format(lastRun) + "%20TO%20*]");
        }

        int pulse = config.path("pulse").asInt(0);

        request.send(ar -> {
            if (ar.succeeded()) {

                HttpResponse<Buffer> response = ar.result();
                switch (response.statusCode()) {
                    case 200:
                        int count;
                        CkanResponse ckanResponse = new CkanResponse(response.bodyAsJsonObject());
                        if (ckanResponse.isSuccess()) {
                            JsonArray results;
                            if (!dialect.equals("dkan")) {
                                CkanObjectResult ckanResult = ckanResponse.getObjectResult();
                                JsonObject content = ckanResult.getContent();
                                count = content.getInteger("count", -1);
                                results = content.getJsonArray("results", content.getJsonArray("result", new JsonArray()));
                            } else {
                                CkanArrayResult ckanResult = ckanResponse.getArrayResult();
                                count = -1;
                                results = ckanResult.getContent();
                            }
                            final int total = count;

                            boolean more = (total == -1 || offset < total) && results.size() > 0;

                            if (pulse > 0) {
                                TimeoutStream stream = vertx.periodicStream(pulse);
                                stream.handler(l -> {
                                    JsonObject dataset = (JsonObject) results.remove(0);
                                    forwardDataset(dataset, pipeContext, total, identifiers);

                                    if (results.isEmpty()) {
                                        stream.cancel();
                                        if (more) {
                                            fetchPage(offset + pageSize, pipeContext, identifiers);
                                        } else {
                                            pipeContext.log().info("Import metadata finished");
                                        }
                                    }
                                });
                            } else {
                                results.forEach(object -> forwardDataset((JsonObject)object, pipeContext, total, identifiers));
                                if (more) {
                                    fetchPage(offset + pageSize, pipeContext, identifiers);
                                } else {
                                    pipeContext.log().info("Import metadata finished");
                                    int delay = pipeContext.getConfig().path("sendListDelay").asInt(defaultDelay);
                                    vertx.setTimer(delay, t -> {
                                        // give last datasets a head start with a chance to arrive at target
                                        ObjectNode info = new ObjectMapper().createObjectNode()
                                                .put("content", "identifierList")
                                                .put("catalogue", config.path("catalogue").asText());
                                        pipeContext.setResult(new JsonArray(identifiers).encodePrettily(), "application/json", info).forward(client);
                                    });
                                }
                            }
                        } else {
                            CkanError ckanError = ckanResponse.getError();
                            pipeContext.log().error(ckanError.getMessage());
                        }
                        break;
                    case 500:
                    default:
                        pipeContext.log().error(response.statusMessage());
                }
            } else {
                pipeContext.setFailure(ar.cause());
            }
        });
    }

    private void forwardDataset(JsonObject dataset, PipeContext pipeContext, int total, List<String> identifiers) {
        pipeContext.getConfig().get("catalogue").asText();
        identifiers.add(dataset.getString("name"));
        ObjectNode dataInfo = new ObjectMapper().createObjectNode()
                .put("total", total)
                .put("counter", identifiers.size())
                .put("identifier", dataset.getString("name"))
                .put("catalogue", pipeContext.getConfig().path("catalogue").asText());
        pipeContext.setResult(dataset.encodePrettily(), "application/json", dataInfo).forward(client);
        pipeContext.log().info("Data imported: {}", dataInfo);
    }

}

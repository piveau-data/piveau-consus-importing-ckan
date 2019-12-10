package io.piveau.importing.ckan.response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class HttpResponse<T> {

    protected final T content;

    protected HttpResponse(T content) {
    	this.content = content;
    }

    protected abstract boolean isError();

    protected abstract boolean isSuccess();

    protected abstract HttpResult<JsonArray> getArrayResult();

    protected abstract HttpResult<JsonObject> getObjectResult();

    protected abstract HttpError<T> getError();

}

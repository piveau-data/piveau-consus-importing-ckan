package io.piveau.importing.ckan.response;

import io.vertx.core.json.JsonObject;

public class CkanObjectResult extends HttpResult<JsonObject> {
	
	public CkanObjectResult(JsonObject result) {
		super(result);
	}

	@Override
	public JsonObject getContent() {
		return result;
	}
	
}

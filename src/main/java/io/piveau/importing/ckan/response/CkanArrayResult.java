package io.piveau.importing.ckan.response;

import io.vertx.core.json.JsonArray;

public class CkanArrayResult extends HttpResult<JsonArray> {

	public CkanArrayResult(JsonArray result) {
		super(result);
	}

	@Override
	public JsonArray getContent() {
		return result;
	}
	
}

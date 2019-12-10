package io.piveau.importing.ckan.response;

import io.vertx.core.json.JsonObject;

public class CkanError extends HttpError<JsonObject> {

	public CkanError(JsonObject error) {
		super(error);
	}

	@Override
	public String getType() {
		return error.getString("__type");
	}

	@Override
	public String getMessage() {
		return error.getString("message");
	}
	
}

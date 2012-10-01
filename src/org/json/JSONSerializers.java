package org.json;

import java.lang.reflect.Type;

import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JSONSerializers {
	public static class OFActionSerializer implements JsonSerializer<OFAction> {
		  public JsonElement serialize(OFAction src, Type typeOfSrc, JsonSerializationContext context) {
		    return context.serialize(src, src.getClass());
		  }
		}

	public static class OFMatchSerializer implements JsonSerializer<OFMatch>{
		public JsonElement serialize(OFMatch src, Type typeOfSrc, JsonSerializationContext context){
			return new JsonPrimitive(src.toString());
		}
	}

	public static class FlowEntrySerializer implements JsonSerializer<FlowEntry>{

		@Override
		public JsonElement serialize(FlowEntry src, Type srcType,
				JsonSerializationContext context) {

			final Gson gson =
				new GsonBuilder().registerTypeAdapter(OFAction.class, new JSONSerializers.OFActionSerializer())
				.registerTypeAdapter(OFMatch.class, new JSONSerializers.OFMatchSerializer()).create();

			// astruble: create a new gso instance that doesn't use this serializer in order to get the default serialization
			// of this object. If we at some point move to a later version of gson, then we can just use
			// context.getDefaultSerialization(), but that method doesn't exist yet in this version.
			JsonElement serialized = gson.toJsonTree(src, srcType);


			JsonObject jsonObject = serialized.getAsJsonObject();
			jsonObject.remove("dpid");
			long dpid = src.getDpid();
			if (dpid == FlowEntry.ALL_DPIDS)
				jsonObject.addProperty("dpid", FlowEntry.ALL_DPIDS_STR);
			else
				jsonObject.addProperty("dpid", FlowSpaceUtil.dpidToString(dpid));

		    return jsonObject;
		}

	}
}

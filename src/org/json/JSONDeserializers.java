package org.json;

import java.lang.reflect.Type;

import org.flowvisor.FlowVisor;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.util.HexString;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class JSONDeserializers {
	public static class OFActionDeserializer implements JsonDeserializer<OFAction> {
		public OFAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
		throws JsonParseException {
			int vendor = json.getAsJsonObject().get("vendor").getAsJsonPrimitive().getAsInt();
			if(vendor == FlowVisor.FLOWVISOR_VENDOR_EXTENSION){
				return context.deserialize(json, SliceAction.class);
			}
			return context.deserialize(json, OFAction.class);
		}
	}

	public static class OFMatchDeserializer implements JsonDeserializer<OFMatch>{
		public OFMatch deserialize(JsonElement json, Type typeOft, JsonDeserializationContext context)
		throws JsonParseException{
			OFMatch match = new OFMatch();
			match.fromString(json.getAsString());
			return match;
		}
	}

	public static class FlowEntryDeserializer implements JsonDeserializer<FlowEntry>{

		@Override
		public FlowEntry deserialize(JsonElement elem, Type arg1,
				JsonDeserializationContext context) throws JsonParseException {

			JsonObject obj = elem.getAsJsonObject();
			String dpidStr = obj.getAsJsonPrimitive("dpid").getAsString();
			long dpid;
			if (dpidStr.equals(FlowEntry.ALL_DPIDS_STR))
				dpid = FlowEntry.ALL_DPIDS;
			else
				dpid = HexString.toLong(dpidStr);

			obj.addProperty("dpid", dpid);

			final Gson gson =
				new GsonBuilder().registerTypeAdapter(OFAction.class, new JSONDeserializers.OFActionDeserializer())
				.registerTypeAdapter(OFMatch.class, new JSONDeserializers.OFMatchDeserializer()).create();

			// astruble: create a new gson instance that doesn't use this deserializer in order to get the default deserialization
			// of this object. If we at some point move to a later version of gson, then we can just use
			// context.getDefaultSerialization(), but that method doesn't exist yet in this version.
			return gson.fromJson(obj, arg1);
		}

	}
}

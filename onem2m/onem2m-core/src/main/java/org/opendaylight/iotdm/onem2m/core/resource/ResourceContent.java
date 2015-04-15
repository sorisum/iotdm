/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the resource content that was supplied in the RequestPrimitive.CONTENT parameter.  It is
 * formatted according to the CONTENT_TYPE.   It is parsed and the parameter are put in the DbAttr list.  Resource
 * specific methods are called based on the resourceType that is being created.
 */
public class ResourceContent {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContent.class);

    public static final String RESOURCE_TYPE = "rty";
    public static final String RESOURCE_ID = "ri";
    public static final String RESOURCE_NAME = "rn";
    public static final String PARENT_ID = "pi";
    public static final String EXPIRATION_TIME = "et";
    public static final String CREATION_TIME = "ct";
    public static final String LAST_MODIFIED_TIME = "lt";
    public static final String LABELS = "lbl";
    public static final String STATE_TAG = "st";
    public static final String CHILD_RESOURCE = "childResource";
    public static final String CHILD_RESOURCE_REF = "childResourceRef";

    private DbAttr dbAttrs;
    private JSONObject jsonContent;
    private String xmlContent;

    public ResourceContent() {
        dbAttrs = new DbAttr();
        jsonContent = null;
        xmlContent = null;
    }

    public boolean isJson() { return jsonContent != null; }
    public JSONObject getJsonContent() { return jsonContent; }
    public boolean isXml() { return xmlContent != null; }

    /**
     * Pulls the json/xml formatted data out of the RequestPrimitive.CONTENT string
     * and put it into the request.  It calls an abstract method so that each resource pulls out the data
     * specific to that resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public void parse(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        String cf = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT_FORMAT);
        switch (cf) {
            case Onem2m.ContentFormat.JSON:
                jsonContent = parseJson(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ContentFormat.XML:
                xmlContent = parseXml(onem2mRequest, onem2mResponse);
                break;
        }
    }

    /**
     * Parse the JSON content, put it into the set of RequestPrimitive attrs by calling the extractJsonRequestContent
     * which is implemented by each of the resource specific classes as it is an abstract method.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private JSONObject parseJson(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        jsonContent = null;
        String jsonContentString = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT);
        if (jsonContentString == null) {
            // TS0004: 7.2.3.2
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") not specified");
            return null;
        }
        try {
            jsonContent = new JSONObject(jsonContentString.trim());
        } catch (JSONException e) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") parser error (" + e + ")");
            return null;
        }

        // the json should be an array of objects called "any", we support only one element in the array so pull
        // that element in the array out and place it in jsonContent
        return processJsonCreateAnyArray(jsonContent, onem2mResponse);
    }

    private JSONObject processJsonCreateAnyArray(JSONObject jAnyArray, ResponsePrimitive onem2mResponse) {

        Iterator<?> keys = jAnyArray.keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            Object o = jAnyArray.get(key);

            switch (key) {

                case "any":
                    if (!(o instanceof JSONArray)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                        return null;
                    }
                    JSONArray array = (JSONArray) o;
                    if (array.length() != 1) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") too many elements: json array length: " +
                                        array.length());
                        return null;
                    }
                    if (!(array.get(0) instanceof JSONObject)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") JSONObject expected");
                        return null;
                    }
                    return (JSONObject) array.get(0);
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: expected: any" + key);
                    return null;
            }
        }
        return null;
    }

    public void processCommonCreateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        this.setDbAttr(ResourceContent.RESOURCE_TYPE,
                onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE));

        // resourceId, resourceName, parentId is filled in by the Onem2mDb.createResource method

        String currDateTime = Onem2mDateTime.getCurrDateTime();

        String ct = this.getDbAttr(ResourceContent.CREATION_TIME);
        if (ct != null) {
            if (!Onem2mDateTime.isValidDateTime(ct)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Invalid ISO8601 date/time format: (YYYYMMDDTHHMMSSZ) " + ct);
                return;
            } else if (Onem2mDateTime.dateCompare(ct, currDateTime) > 0) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Cannot set time in the future " + ct);
                return;
            }
        } else {
            this.setDbAttr(ResourceContent.CREATION_TIME, currDateTime);
        }

        // if expiration time not provided, make one
        String et = this.getDbAttr(ResourceContent.EXPIRATION_TIME);
        if (et != null) {
            if (!Onem2mDateTime.isValidDateTime(et)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Invalid ISO8601 date/time format: (YYYYMMDDTHHMMSSZ) " + et);
                return;
            } else if (Onem2mDateTime.dateCompare(et, currDateTime) < 0) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Cannot set time in the past " + et);
                return;
            }
        } else {
            this.setDbAttr(ResourceContent.EXPIRATION_TIME, currDateTime /* + ONEM2M_FUTURE_EXPIRY */);
        }

        String lmt = this.getDbAttr(ResourceContent.LAST_MODIFIED_TIME);
        if (lmt != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "LAST_MODIFIED_TIME: read-only parameter");
            return;
        }
        this.setDbAttr(ResourceContent.LAST_MODIFIED_TIME, currDateTime);
    }

    public static void produceJsonForCommonAttributes(Attr attr, JSONObject j) {

        switch (attr.getName()) {
            case ResourceContent.CREATION_TIME:
            case ResourceContent.EXPIRATION_TIME:
            case ResourceContent.LAST_MODIFIED_TIME:
                j.put(attr.getName(), attr.getValue());
                break;
            case ResourceContent.RESOURCE_TYPE:
                // TODO: should this be a number
                //j.put(attr.getName(), attr.getValue());
                j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                break;
            case ResourceContent.STATE_TAG:
                j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                break;
        }
    }

    public static void produceJsonForCommonAttributeSets(AttrSet attrSet, JSONObject j) {

        switch (attrSet.getName()) {
            case ResourceContent.LABELS:
                JSONArray a = new JSONArray();
                for (Member member : attrSet.getMember()) {
                    a.put(member.getValue());
                }
                j.put(attrSet.getName(), a);
                break;
        }
    }

    public static void produceJsonForResource(String resourceType, Onem2mResource onem2mResource, JSONObject j) {

        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.produceJsonForResource(onem2mResource, j);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.produceJsonForResource(onem2mResource, j);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.produceJsonForResource(onem2mResource, j);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.produceJsonForResource(onem2mResource, j);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.produceJsonForResource(onem2mResource, j);
                break;
        }
    }

    /**
     * As soon as we start supporting xml content, each resource will have to implement a method similar to extractJsonRequestContent
     * and it will be called something like extractXmlRequestContent and will be called from parseXmlContent.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private String parseXml(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                "CONTENT(" + RequestPrimitive.CONTENT + ") xml not supported yet");
        return null;
    }

    public String getDbAttr(String name) {
        return this.dbAttrs.getAttr(name);
    }

    public void setDbAttr(String name, String value) {
        this.dbAttrs.setAttr(name, value);
    }

    public DbAttr getDbAttrList() {
        return this.dbAttrs;
    }

    public List<Attr> getAttrList() {
        return this.dbAttrs.getAttrList();
    }

    public void setAttrList(List<Attr> attrList) {
        this.dbAttrs = new DbAttr(attrList);
    }
}

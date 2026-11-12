/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.exception.ExceptionAdditionalInfo;
import com.eqixiac.equinix.core.exception.ExceptionDetail;
import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses the error bodies of every Equinix service into {@link ExceptionDetail}s. The services do
 * not share one error format; each shape below was captured live on 2026-11-11 or is the spec's
 * documented error schema. Unknown keys are ignored, and nothing here ever throws.
 *
 * <table>
 *   <caption>Recognised error formats</caption>
 *   <tr><th>Service</th><th>Shape</th></tr>
 *   <tr><td>Fabric, Network Edge, EIA, Resource Manager (spec {@code ErrorList})</td>
 *       <td>{@code [{"errorCode","errorMessage","details","correlationId","help","additionalInfo":[{"property","reason"}]}]}</td></tr>
 *   <tr><td>IAM / Access Management (spec {@code ErrorResponse}, live)</td>
 *       <td>{@code {"error":{"errorCode","message","details":{"projectId":["..."]}}}}</td></tr>
 *   <tr><td>STS (RFC 6749 section 5.2, live snake_case)</td>
 *       <td>{@code {"error":"invalid_scope","error_description":"..."}} (also {@code errorDescription})</td></tr>
 *   <tr><td>Operations APIs (live)</td>
 *       <td>{@code {"errors":[{"code","fields":["/filters/productTypes/0"],"message","messageParams"}]}}</td></tr>
 *   <tr><td>Smart Hands (live)</td><td>{@code {"errors":["text", ...],"ticketId":"..."}}</td></tr>
 *   <tr><td>IBX SmartView v2 (live)</td>
 *       <td>{@code [{"errorCode","title","message","developerMessage","fieldName"}]}</td></tr>
 *   <tr><td>IBX SmartView v1 legacy (live)</td>
 *       <td>{@code {"payLoad":"...","status":{"error":{"msg":"INVALID_INPUT","code":3001},"type":"ERROR"}}}</td></tr>
 *   <tr><td>API gateway, e.g. an {@code api.equinix.com} 404 (live)</td>
 *       <td>{@code {"errorCode","errorMessage","errorDomain","errorTitle","developerMessage"}}</td></tr>
 *   <tr><td>Apigee proxy faults</td><td>{@code {"fault":{"faultstring":"...","detail":{"errorcode":"..."}}}}</td></tr>
 * </table>
 *
 * <p>A body that is not JSON (an HTML 504 page, plain text) or is JSON of no recognised shape
 * yields no details; the caller keeps a {@link #snippet truncated raw snippet} instead.</p>
 *
 * @author ianjones
 */
final class ErrorBodyParser {

    /** Maximum length of the raw-body snippet kept for unparseable bodies. */
    static final int SNIPPET_MAX_CHARS = 500;

    private ErrorBodyParser() {
    }

    /**
     * Parses an error body.
     *
     * @param body the raw body; may be {@code null} or blank
     * @return the parsed details, empty when nothing could be recognised; never {@code null}
     */
    static List<ExceptionDetail> parse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = Constants.mapper().readTree(body);
        } catch (Exception notJson) {
            return List.of();
        }
        try {
            List<ExceptionDetail> details = new ArrayList<>();
            if (root == null) {
                return List.of();
            }
            if (root.isArray()) {
                addEntries(root, details);
            } else if (root.isObject()) {
                parseObject(root, details);
            }
            return details;
        } catch (RuntimeException unexpected) {
            // Defensive: a pathological shape must never replace the API error with a parser error.
            return List.of();
        }
    }

    /**
     * Returns a whitespace-collapsed snippet of the body, at most {@link #SNIPPET_MAX_CHARS}
     * characters (with a trailing ellipsis when truncated), or {@code null} for an empty body.
     */
    static String snippet(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String collapsed = body.strip().replaceAll("\\s+", " ");
        if (collapsed.length() <= SNIPPET_MAX_CHARS) {
            return collapsed;
        }
        return collapsed.substring(0, SNIPPET_MAX_CHARS) + "...";
    }

    private static void parseObject(JsonNode root, List<ExceptionDetail> details) {
        JsonNode errors = root.get("errors");
        if (errors != null && errors.isArray()) {
            // Operations {"errors":[{...}]} and Smart Hands {"errors":["text"],"ticketId":"..."}.
            String ticketId = text(root, "ticketId");
            int before = details.size();
            addEntries(errors, details);
            if (ticketId != null) {
                for (int i = before; i < details.size(); i++) {
                    ExceptionDetail d = details.get(i);
                    if (d.getDetails() == null) {
                        details.set(i, copyWithDetails(d, "ticketId: " + ticketId));
                    }
                }
            }
            return;
        }

        JsonNode error = root.get("error");
        if (error != null && error.isObject()) {
            // IAM {"error":{"errorCode","message","details":{...}}}.
            addIfMeaningful(fromObject(error), details);
            return;
        }
        if (error != null && error.isTextual()) {
            // STS / OAuth {"error":"invalid_scope","error_description":"..."}.
            details.add(ExceptionDetail.builder()
                    .errorCode(error.asText())
                    .errorMessage(firstText(root, "error_description", "errorDescription", "message"))
                    .help(text(root, "error_uri"))
                    .build());
            return;
        }

        JsonNode fault = root.get("fault");
        if (fault != null && fault.isObject()) {
            // Apigee {"fault":{"faultstring":"...","detail":{"errorcode":"..."}}}.
            JsonNode faultDetail = fault.path("detail");
            details.add(ExceptionDetail.builder()
                    .errorCode(firstText(faultDetail, "errorcode", "errorCode"))
                    .errorMessage(text(fault, "faultstring"))
                    .build());
            return;
        }

        JsonNode status = root.get("status");
        if (status != null && status.isObject() && status.path("error").isObject()) {
            // IBX SmartView v1 {"payLoad":"...","status":{"error":{"msg","code"},"type":"ERROR"}}.
            JsonNode statusError = status.path("error");
            JsonNode payLoad = root.get("payLoad");
            String msg = text(statusError, "msg");
            details.add(ExceptionDetail.builder()
                    .errorCode(text(statusError, "code"))
                    .errorTitle(msg)
                    .errorMessage(payLoad != null && payLoad.isTextual() ? payLoad.asText() : msg)
                    .build());
            return;
        }

        // A single error object: the API gateway shape, or any of the array element shapes
        // returned unwrapped.
        addIfMeaningful(fromObject(root), details);
    }

    private static void addEntries(JsonNode array, List<ExceptionDetail> details) {
        for (JsonNode entry : array) {
            if (entry == null || entry.isNull()) {
                continue;
            }
            if (entry.isTextual()) {
                if (!entry.asText().isBlank()) {
                    details.add(ExceptionDetail.builder().errorMessage(entry.asText()).build());
                }
            } else if (entry.isObject()) {
                addIfMeaningful(fromObject(entry), details);
            }
        }
    }

    /**
     * Maps one error object of any format onto a detail. Key synonyms are tried in order; the
     * first present, non-blank value wins.
     */
    private static ExceptionDetail fromObject(JsonNode node) {
        List<ExceptionAdditionalInfo> additionalInfo = new ArrayList<>();
        JsonNode info = node.get("additionalInfo");
        if (info != null && info.isArray()) {
            for (JsonNode entry : info) {
                if (entry != null && entry.isObject()) {
                    additionalInfo.add(new ExceptionAdditionalInfo(
                            text(entry, "property"), text(entry, "reason"), text(entry, "value")));
                }
            }
        }

        JsonNode detailsNode = node.get("details");
        String property = firstText(node, "property", "fieldName", "field");
        if (property == null) {
            property = joinedTexts(node.get("fields"), ", ");
        }
        if (property == null && detailsNode != null && detailsNode.isObject() && detailsNode.size() > 0) {
            // IAM: {"details":{"projectId":["..."]}} names the offending field by its key.
            property = String.join(", ", fieldNames(detailsNode));
        }
        if (property == null && !additionalInfo.isEmpty()) {
            property = additionalInfo.get(0).getProperty();
        }

        return ExceptionDetail.builder()
                .errorCode(firstText(node, "errorCode", "code", "errorcode", "error_code"))
                .errorMessage(firstText(node, "errorMessage", "message", "error_description",
                        "errorDescription", "faultstring"))
                .details(renderDetails(detailsNode))
                .property(property)
                .correlationId(firstText(node, "correlationId", "correlation_id"))
                .errorDomain(text(node, "errorDomain"))
                .errorTitle(firstText(node, "errorTitle", "title"))
                .developerMessage(text(node, "developerMessage"))
                .help(firstText(node, "help", "moreInfo"))
                .additionalInfo(additionalInfo)
                .build();
    }

    private static void addIfMeaningful(ExceptionDetail detail, List<ExceptionDetail> details) {
        if (detail.getErrorCode() != null || detail.getErrorMessage() != null
                || detail.getErrorTitle() != null || detail.getDeveloperMessage() != null
                || detail.getDetails() != null || !detail.getAdditionalInfo().isEmpty()) {
            details.add(detail);
        }
    }

    private static ExceptionDetail copyWithDetails(ExceptionDetail d, String newDetails) {
        return ExceptionDetail.builder()
                .errorCode(d.getErrorCode())
                .errorMessage(d.getErrorMessage())
                .details(newDetails)
                .property(d.getProperty())
                .correlationId(d.getCorrelationId())
                .errorDomain(d.getErrorDomain())
                .errorTitle(d.getErrorTitle())
                .developerMessage(d.getDeveloperMessage())
                .help(d.getHelp())
                .additionalInfo(d.getAdditionalInfo())
                .build();
    }

    /** Renders a {@code details} value: text as is; objects as {@code key: v1; v2}; arrays joined. */
    private static String renderDetails(JsonNode detailsNode) {
        if (detailsNode == null || detailsNode.isNull() || detailsNode.isMissingNode()) {
            return null;
        }
        if (detailsNode.isValueNode()) {
            String value = detailsNode.asText();
            return value.isBlank() ? null : value;
        }
        if (detailsNode.isArray()) {
            return joinedTexts(detailsNode, "; ");
        }
        List<String> parts = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = detailsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String value = field.getValue().isArray() ? joinedTexts(field.getValue(), "; ")
                    : field.getValue().isValueNode() ? field.getValue().asText() : field.getValue().toString();
            parts.add(value == null ? field.getKey() : field.getKey() + ": " + value);
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static List<String> fieldNames(JsonNode object) {
        List<String> names = new ArrayList<>();
        object.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String joinedTexts(JsonNode array, String separator) {
        if (array == null || !array.isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode element : array) {
            if (element != null && !element.isNull()) {
                String value = element.isValueNode() ? element.asText() : element.toString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values.isEmpty() ? null : String.join(separator, values);
    }

    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = text(node, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /** A scalar member as text ({@code null} for absent, JSON null, blank, or non-scalar values). */
    private static String text(JsonNode node, String key) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(key);
        if (value == null || !value.isValueNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}

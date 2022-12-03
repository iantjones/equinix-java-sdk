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

package api.equinix.javasdk.core.internal;

import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.core.model.deserializers.*;
import api.equinix.javasdk.core.enums.Side;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.BitSet;
import java.util.regex.Pattern;

/**
 * <p>Constants class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class Constants {
    /** Constant <code>DEFAULT_ENCODING="UTF-8"</code> */
    public static final String DEFAULT_ENCODING = "UTF-8";
    /** Constant <code>UTF8</code> */
    public static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);
    /** Constant <code>RADIX=16</code> */
    public static final int RADIX = 16;
    /** Constant <code>URL_ENCODER</code> */
    public static final BitSet URL_ENCODER = new BitSet(256);

    public static final Integer PAGE_LIMIT = 2000;
    public static final Integer PAGE_OFFSET = 0;
    public static final Integer PAGE_TOTAL = 0;

    /** Constant <code>BANDWIDTH_CONVERSION_FACTOR</code> */
    public static final Integer BANDWIDTH_CONVERSION_FACTOR = 1000;

    /** Constant <code>LIFECYCLE_DETAIL_FILTER</code> */
    public static final SimpleBeanPropertyFilter LIFECYCLE_DETAIL_FILTER =
            SimpleBeanPropertyFilter.serializeAllExcept("createdBy","createdByFullName","createdByEmail","createdDate",
                    "lastUpdatedBy","lastUpdatedByFullName","lastUpdatedByEmail","updatedByEmail","lastUpdatedDate",
                    "deletedBy","deletedByEmail","deletedDate");

    /** Constant <code>JSON_CONVERTOR</code> */
    public static final ObjectMapper JSON_CONVERTOR = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final SimpleModule module = new SimpleModule()
//            .addDeserializer(EncapsulationType.class, new EncapsulationDeserializer())
            .addDeserializer(BandwidthUnit.class, new BandwidthDeserializer())
            .addDeserializer(Side.class, new SideDeserializer())
            .addDeserializer(OperationalStatus.class, new OperationalStatusDeserializer())
            .addDeserializer(MetroCode.class, new MetroCodeDeserializer());

    /** Constant <code>objectMapper</code> */
    public static ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(MapperFeature.USE_STD_BEAN_NAMING)
            .registerModule(new Jdk8Module())
            .registerModule(module)
            .setFilterProvider(new SimpleFilterProvider()
                    .addFilter("lifecycleDetailFilter", LIFECYCLE_DETAIL_FILTER))
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Constant <code>queryParamFormatter</code> */
    public static final DateTimeFormatter queryParamFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /** Constant <code>DATE_TIME_FORMAT</code> */
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][' ']HH:mm:ss[.SSSSSS][.SSS][.SS][.S][X]");
    /** Constant <code>COMMENCE_BILLING</code> */
    public static final DateTimeFormatter COMMENCE_BILLING = DateTimeFormatter.ofPattern("EEE LLL dd yyyy HH:mm:ss zzz");
    /** Constant <code>COMMENCE_BILLING_SHORT</code> */
    public static final DateTimeFormatter COMMENCE_BILLING_SHORT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    /** Constant <code>ALL_DATE_FORMATS</code> */
    public static final DateTimeFormatter ALL_DATE_FORMATS = new DateTimeFormatterBuilder()
            .appendOptional(COMMENCE_BILLING)
            .appendOptional(COMMENCE_BILLING_SHORT)
            .appendOptional(DATE_TIME_FORMAT)
            .toFormatter();

    /** Constant <code>IP_SUBNET_PATTERN="^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([0"{trunked}</code> */
    public static final String IP_SUBNET_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])(\\/?)([1238]{1}[0-9]{1})?$";

    private static final String UUID_FORMAT = ".*[{\\[]?([0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})[]}]?";
    /** Constant <code>UUID_PATTERN</code> */
    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_FORMAT);

    /** Constant <code>JSON_DESERIALIZE_EXCEPTION="Error mapping EquinixResponse from Apac"{trunked}</code> */
    public static final String JSON_DESERIALIZE_EXCEPTION = "Error mapping EquinixResponse from Apache Response Content.";
    /** Constant <code>JSON_SERIALIZE_EXCEPTION="Error serializing EquinixRequest to JSO"{trunked}</code> */
    public static final String JSON_SERIALIZE_EXCEPTION = "Error serializing EquinixRequest to JSON.";

}

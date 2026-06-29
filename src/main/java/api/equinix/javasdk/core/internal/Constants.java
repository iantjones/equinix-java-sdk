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
import java.util.ServiceLoader;
import java.util.regex.Pattern;

/**
 *
 * @author ianjones
 */
public class Constants {
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);
    public static final int RADIX = 16;
    public static final BitSet URL_ENCODER = new BitSet(256);

    // Equinix list/search endpoints cap page size (Fabric v4 max = 100); 2000 was rejected on real calls.
    public static final Integer PAGE_LIMIT = 100;
    public static final Integer PAGE_OFFSET = 0;
    public static final Integer PAGE_TOTAL = 0;

    public static final Integer BANDWIDTH_CONVERSION_FACTOR = 1000;

    public static final SimpleBeanPropertyFilter LIFECYCLE_DETAIL_FILTER =
            SimpleBeanPropertyFilter.serializeAllExcept("createdBy","createdByFullName","createdByEmail","createdDate",
                    "lastUpdatedBy","lastUpdatedByFullName","lastUpdatedByEmail","updatedByEmail","lastUpdatedDate",
                    "deletedBy","deletedByEmail","deletedDate");

    public static final ObjectMapper JSON_CONVERTOR = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final SimpleModule module = new SimpleModule()
            .addDeserializer(BandwidthUnit.class, new BandwidthDeserializer())
            .addDeserializer(OperationalStatus.class, new OperationalStatusDeserializer())
            .addDeserializer(MetroCode.class, new MetroCodeDeserializer());

    public static final ObjectMapper objectMapper = buildObjectMapper();

    /**
     * Builds the shared {@link ObjectMapper}, registering the core module plus any
     * domain-contributed modules discovered via the {@link JacksonModuleProvider} SPI
     * (so core carries no dependency on domain-specific (de)serializers).
     */
    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
                // Forward-compatibility for enums: a value the SDK doesn't know maps to the enum's
                // @JsonEnumDefaultValue when one is declared, otherwise to null — never failing the
                // whole response. The Equinix APIs add enum values (states, change types, etc.) over
                // time; without this a single unrecognized value would crash an otherwise-valid read.
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // NOTE: USE_STD_BEAN_NAMING was removed — combined with Lombok getters for fields
                // like `aEndIbx` (getter getAEndIbx()), it derived a phantom capitalized property
                // ("AEndIbx") in addition to the @JsonProperty("aEndIbx"), emitting duplicate keys
                // in request bodies. Default naming derives "aEndIbx", matching the annotation.
                .registerModule(new Jdk8Module())
                .registerModule(module)
                .setFilterProvider(new SimpleFilterProvider()
                        .addFilter("lifecycleDetailFilter", LIFECYCLE_DETAIL_FILTER))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        for (JacksonModuleProvider provider : ServiceLoader.load(
                JacksonModuleProvider.class, Constants.class.getClassLoader())) {
            mapper.registerModule(provider.getModule());
        }

        return mapper;
    }

    public static final DateTimeFormatter queryParamFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][' ']HH:mm:ss[.SSSSSS][.SSS][.SS][.S][X]");
    public static final DateTimeFormatter COMMENCE_BILLING = DateTimeFormatter.ofPattern("EEE LLL dd yyyy HH:mm:ss zzz");
    public static final DateTimeFormatter COMMENCE_BILLING_SHORT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    public static final DateTimeFormatter ALL_DATE_FORMATS = new DateTimeFormatterBuilder()
            .appendOptional(COMMENCE_BILLING)
            .appendOptional(COMMENCE_BILLING_SHORT)
            .appendOptional(DATE_TIME_FORMAT)
            .toFormatter();

    public static final String IP_SUBNET_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])(\\/?)([1238]{1}[0-9]{1})?$";

    private static final String UUID_FORMAT = ".*[{\\[]?([0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})[]}]?";
    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_FORMAT);

    public static final String JSON_DESERIALIZE_EXCEPTION = "Error mapping EquinixResponse from Apache Response Content.";
    public static final String JSON_SERIALIZE_EXCEPTION = "Error serializing EquinixRequest to JSON.";

}

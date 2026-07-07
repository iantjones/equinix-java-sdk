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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

/**
 *
 * @author ianjones
 */
public final class Constants {

    private Constants() {
        // static holder — not instantiable
    }

    public static final String DEFAULT_ENCODING = "UTF-8";

    // Equinix list/search endpoints cap page size (Fabric v4 max = 100); 2000 was rejected on real calls.
    public static final Integer PAGE_LIMIT = 100;
    public static final Integer PAGE_OFFSET = 0;

    public static final SimpleBeanPropertyFilter LIFECYCLE_DETAIL_FILTER =
            SimpleBeanPropertyFilter.serializeAllExcept("createdBy","createdByFullName","createdByEmail","createdDate",
                    "lastUpdatedBy","lastUpdatedByFullName","lastUpdatedByEmail","updatedByEmail","lastUpdatedDate",
                    "deletedBy","deletedByEmail","deletedDate");

    // Used only for object-to-object mapping (e.g. seeding a resource's *UpdaterJson from its current
    // *Json via convertValue). REQUIRE_HANDLERS_FOR_JAVA8_TIMES is disabled because a source *Json can
    // expose read-only java.time lifecycle getters (createdDate/lastUpdatedDate/deletedDate) that this
    // bare mapper has no serializer for; they are not part of any updater, so they are dropped on the
    // target — without this, seeding an update() from a fetched resource with a populated timestamp
    // would throw. Actual wire serialization/deserialization uses {@link #mapper()}, not this.
    // Fully configured here at construction; never reconfigured afterwards (thread-safety contract).
    private static final ObjectMapper CONVERTER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES, false);

    private static final SimpleModule module = new SimpleModule()
            .addDeserializer(BandwidthUnit.class, new BandwidthDeserializer())
            .addDeserializer(OperationalStatus.class, new OperationalStatusDeserializer())
            .addDeserializer(MetroCode.class, new MetroCodeDeserializer());

    // The single wire (de)serialization mapper for the whole SDK. Private so no consumer can
    // reconfigure global SDK behaviour (ObjectMapper is only thread-safe once its configuration
    // is frozen); read access is via {@link #mapper()}.
    private static final ObjectMapper WIRE_MAPPER = buildObjectMapper();

    /**
     * The shared wire {@link ObjectMapper} used for all SDK request/response (de)serialization.
     *
     * <p><strong>Do not reconfigure the returned instance.</strong> Its configuration is frozen at
     * class-initialization time and it is used concurrently by every SDK thread; calling
     * {@code configure}/{@code registerModule}/{@code setSerializationInclusion} (or similar) on it
     * is a thread-safety violation and silently changes SDK-wide behaviour. Treat it as read-only:
     * use it for {@code readValue}/{@code writeValueAsString}/{@code convertValue} or derive an
     * immutable handle via {@code reader()}/{@code writer()}.</p>
     *
     * @return the shared, fully configured wire mapper
     */
    public static ObjectMapper mapper() {
        return WIRE_MAPPER;
    }

    /**
     * The shared object-to-object conversion {@link ObjectMapper} (used to seed updater JSON models
     * from fetched resources via {@code convertValue}; never used for wire traffic).
     *
     * <p><strong>Do not reconfigure the returned instance</strong> — same contract as {@link #mapper()}.</p>
     *
     * @return the shared, fully configured conversion mapper
     */
    public static ObjectMapper converter() {
        return CONVERTER;
    }

    /**
     * Builds the shared {@link ObjectMapper}, registering the core module plus any
     * domain-contributed modules discovered via the {@link JacksonModuleProvider} SPI
     * (so core carries no dependency on domain-specific (de)serializers). The mapper is
     * fully configured before it is published via {@link #mapper()} and is never mutated
     * afterwards.
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

    /**
     * Formats a date-time query/body parameter in the API's {@code yyyy-MM-dd'T'HH:mm:ss'Z'} shape.
     *
     * <p><strong>The input must already represent UTC.</strong> The trailing {@code 'Z'} is a
     * literal, not an offset: this formatter performs no zone conversion, so formatting a
     * zone-local {@link java.time.LocalDateTime} directly would stamp local wall-clock digits with
     * a UTC designator. Callers must convert first, e.g.
     * {@code localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()}
     * or format an {@link java.time.Instant} via {@code LocalDateTime.ofInstant(instant, ZoneOffset.UTC)}.</p>
     */
    public static final DateTimeFormatter queryParamFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][' ']HH:mm:ss[.SSSSSS][.SSS][.SS][.S][X]");
    // Locale is pinned to ENGLISH: the wire emits English day/month/zone names
    // ("Wed Mar 03 2021 14:00:00 GMT"); without an explicit locale the EEE/LLL/zzz text tokens
    // resolve against the JVM default format locale and parsing fails on e.g. de_DE hosts.
    public static final DateTimeFormatter COMMENCE_BILLING =
            DateTimeFormatter.ofPattern("EEE LLL dd yyyy HH:mm:ss zzz", Locale.ENGLISH);
    public static final DateTimeFormatter COMMENCE_BILLING_SHORT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    public static final DateTimeFormatter ALL_DATE_FORMATS = new DateTimeFormatterBuilder()
            .appendOptional(COMMENCE_BILLING)
            .appendOptional(COMMENCE_BILLING_SHORT)
            .appendOptional(DATE_TIME_FORMAT)
            .toFormatter();

    private static final String UUID_FORMAT = ".*[{\\[]?([0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})[]}]?";
    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_FORMAT);

    public static final String JSON_DESERIALIZE_EXCEPTION = "Error mapping EquinixResponse from Apache Response Content.";
    public static final String JSON_SERIALIZE_EXCEPTION = "Error serializing EquinixRequest to JSON.";

}

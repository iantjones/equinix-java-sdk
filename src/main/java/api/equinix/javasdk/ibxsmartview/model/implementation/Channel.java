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

package api.equinix.javasdk.ibxsmartview.model.implementation;

import api.equinix.javasdk.ibxsmartview.enums.ChannelType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The single delivery channel of a streaming subscription. Carries the channel type and the
 * matching typed channel configuration (AWS IoT Core, Webhook or Azure).
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Channel {

    @JsonProperty("channelType")
    private ChannelType channelType;

    @JsonProperty("awsIotCoreChannelConfiguration")
    private AwsIotCoreChannelConfiguration awsIotCoreChannelConfiguration;

    @JsonProperty("webhookChannelConfiguration")
    private WebhookChannelConfiguration webhookChannelConfiguration;

    @JsonProperty("azureChannelConfiguration")
    private AzureChannelConfiguration azureChannelConfiguration;

    @Builder
    public Channel(ChannelType channelType, AwsIotCoreChannelConfiguration awsIotCoreChannelConfiguration,
                   WebhookChannelConfiguration webhookChannelConfiguration,
                   AzureChannelConfiguration azureChannelConfiguration) {
        this.channelType = channelType;
        this.awsIotCoreChannelConfiguration = awsIotCoreChannelConfiguration;
        this.webhookChannelConfiguration = webhookChannelConfiguration;
        this.azureChannelConfiguration = azureChannelConfiguration;
    }
}

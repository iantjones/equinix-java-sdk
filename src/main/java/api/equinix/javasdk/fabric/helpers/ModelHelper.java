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

package api.equinix.javasdk.fabric.helpers;

import api.equinix.javasdk.fabric.enums.ServiceTokenState;
import api.equinix.javasdk.fabric.model.ServiceToken;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>ModelHelper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ModelHelper extends api.equinix.javasdk.core.helpers.ModelHelper {

    /**
     * <p>filterServiceTokensByCreator.</p>
     *
     * @param serviceTokens a {@link java.util.List} object.
     * @param createdBy a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<ServiceToken> filterServiceTokensByCreator(List<ServiceToken> serviceTokens, String createdBy) {
        return serviceTokens.stream()
                .filter(serviceToken -> serviceToken.getChangeLog().getCreatedBy().equals(createdBy))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * <p>filterOutServiceTokensByState.</p>
     *
     * @param serviceTokens a {@link java.util.List} object.
     * @param serviceTokenState a {@link api.equinix.javasdk.fabric.enums.ServiceTokenState} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<ServiceToken> filterOutServiceTokensByState(List<ServiceToken> serviceTokens, ServiceTokenState serviceTokenState) {
        return serviceTokens.stream()
                .filter(serviceToken -> serviceToken.getState() != serviceTokenState)
                .collect(Collectors.toCollection(ArrayList::new));
    }

//
//    public static ArrayList<MetroJson> findConnectedMetrosWithoutLatency(List<MetroJson> metros) {
//        Predicate<MetroJson> avgLatencyIsNull =
//                metro -> metro.getConnectedMetros().stream()
//                        .anyMatch(connectedMetro -> connectedMetro.getAvgLatency() == null);
//
//        return metros.stream()
//                .filter(metro -> metro.getConnectedMetros() != null)
//                .filter(avgLatencyIsNull)
//                .collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    public static Port findPortByUuid(List<Port> ports, String portUuid) {
//        return ports.stream()
//                .filter(port -> portUuid.equals(port.getPortUuid()))
//                .findAny().orElse(null);
//    }
//
//    public static ArrayList<Port> findPortsByMetroCode(List<Port> ports, MetroCode metroCode) {
//        return ports.stream()
//                .filter(port -> metroCode.equals(port.getMetroCode()))
//                .collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    public static ArrayList<Service> findServicesByMetroCode(List<Service> services, MetroCode metroCode) {
//        Predicate<Service> containsDesiredMetro =
//                service -> service.getMetros().stream().anyMatch(metro -> metro.getMetroCode().equals(metroCode));
//
//        return services.stream()
//                .filter(containsDesiredMetro)
//                .collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    public static ArrayList<Connection> findConnectionsByStatus(List<Connection> connections, ConnectionStatus connectionStatus) {
//        return connections.stream()
//                .filter(connection -> connectionStatus.equals(connection.getConnectionStatus()))
//                .collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    public static ArrayList<Service> findL2ServiceProfilesByOrgName(List<Service> services, String organizationName) {
//        return services.stream()
//                .filter(l2ServiceProfile -> l2ServiceProfile.getOrganizationName() != null)
//                .filter(l2ServiceProfile -> l2ServiceProfile.getOrganizationName().contains(organizationName))
//                .collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    public static ArrayList<Connection> findConnectionsWithoutVirtualDevice(List<Connection> connections) {
//        return connections.stream()
//                .filter(connection -> connection.getVirtualDeviceUuid() == null)
//                .collect(Collectors.toCollection(ArrayList::new));
//    }
//
//    public static Double aggregateConnectionBandwith(Port port, List<Connection> connections, BandwidthUnit desiredBandwidthUnit) {
//        Predicate<Connection> hasPort = connection -> connection.getPortUuid() != null;
//        Predicate<Connection> onPort = connection -> connection.getPortUuid().equals(port.getPortUuid());
//
//        Map<BandwidthUnit, Integer> connectionsOnPort = connections.stream()
//                .filter(hasPort)
//                .filter(onPort)
//                .collect(Collectors.toMap(
//                        Connection::getBandwidthUnit,
//                        Connection::getSpeed,
//                        Integer::sum));
//
//        return normalizeAndSumBandwidth(connectionsOnPort, desiredBandwidthUnit);
//    }
//
//    public static Double normalizeAndSumBandwidth(Map<BandwidthUnit, Integer> connectionBandwidth, BandwidthUnit desiredBandwidthUnit) {
//        return connectionBandwidth.entrySet().stream()
//                .map(c -> bandwidthAs(c.getValue(), c.getKey(), desiredBandwidthUnit))
//                .reduce(0.0, Double::sum);
//    }
//
//    public static Double bandwidthAs(Integer bandwidth, BandwidthUnit bandwidthUnit, BandwidthUnit desiredBandwidthUnit) {
//        int factor = bandwidthUnit.getConversationFactor() - desiredBandwidthUnit.getConversationFactor();
//        double totalFactor = Math.pow(Constants.BANDWIDTH_CONVERSION_FACTOR, Math.abs(factor));
//
//        if(factor < 0) {
//            return (bandwidth / totalFactor);
//        }
//        else if (factor > 0) {
//            return (bandwidth * totalFactor);
//        }
//        else {
//            return (double)bandwidth;
//        }
//    }
}

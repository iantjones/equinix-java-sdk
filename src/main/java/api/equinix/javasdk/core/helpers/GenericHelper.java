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

package api.equinix.javasdk.core.helpers;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.IPAddress;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>GenericHelper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class GenericHelper {

    /**
     * <p>outputInputStream.</p>
     *
     * @param inputStream a {@link java.io.InputStream} object.
     */
    public static void outputInputStream(InputStream inputStream) {
        try {
            System.out.println(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * <p>stringToIPAddress.</p>
     *
     * @param rawIp a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.core.model.IPAddress} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public static IPAddress stringToIPAddress(String rawIp) throws EquinixClientException {

        String parsedIp;
        String subnet;
        IPAddress ipAddress;

        Pattern pattern = Pattern.compile(Constants.IP_SUBNET_PATTERN, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(rawIp);

        try {
            ipAddress = new IPAddress();
            while(matcher.find()) {
                parsedIp = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3) + "." + matcher.group(4);
                ipAddress.setIpAddress(InetAddress.getByName(parsedIp));

                if (matcher.groupCount() == 6 && matcher.group(6) != null) {
                    subnet = matcher.group(6);
                    ipAddress.setSubnet(Integer.parseInt(subnet));
                }
            }
        }
        catch (UnknownHostException uhe) {
            throw new EquinixClientException("Failed to convert raw IP Address String to com.equinix.api.model.IPAddress Object.", uhe);
        }

        return ipAddress;
    }

    /**
     * <p>capitalizeFirst.</p>
     *
     * @param input a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String capitalizeFirst(String input) {
        return (input != null && input.length() >= 2) ? input.substring(0, 1).toUpperCase() + input.substring(1) : null;
    }
}

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

package api.equinix.javasdk.core.exception;

import api.equinix.javasdk.core.util.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;

/**
 * <p>EquinixServiceException class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
public class EquinixServiceException extends EquinixClientException {

    private ArrayList<ExceptionDetail> exceptionDetails = new ArrayList<>();
    private Integer statusCode;
    private Map<String, String> httpHeaders;
    private String path;

    /**
     * <p>Constructor for EquinixServiceException.</p>
     *
     * @param errorMessage a {@link java.lang.String} object.
     */
    public EquinixServiceException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * <p>Constructor for EquinixServiceException.</p>
     *
     * @param errorMessage a {@link java.lang.String} object.
     * @param cause a {@link java.lang.Exception} object.
     */
    public EquinixServiceException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    /**
     * <p>getMessage.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMessage() {
        StringBuilder errorString = new StringBuilder();

        errorString.append("Status Code: ").append(getStatusCode()).append("\nURI: ").append(getPath());

        for(ExceptionDetail exceptionDetail : exceptionDetails) {
            errorString.append(!StringUtils.isNullOrEmpty(exceptionDetail.getErrorMessage()) ? "\nError Message: " + exceptionDetail.getErrorMessage() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getFault()) ? "\nFault: " + exceptionDetail.getFault() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getError()) ? "\nError Message: " + exceptionDetail.getError() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getDetails()) ? "\nDetails: " + exceptionDetail.getDetails() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getMoreInfo()) ? "\nMore Info: " + exceptionDetail.getMoreInfo() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getErrorCode()) ? "\nError Code: " + exceptionDetail.getErrorCode() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getCorrelationId()) ? "\nCorrelation Id: " + exceptionDetail.getCorrelationId() : "")
                    .append(!StringUtils.isNullOrEmpty(exceptionDetail.getPath()) ? "\nURI: " + exceptionDetail.getPath() : "");

            for(ExceptionAdditionalInfo exceptionAdditionalInfo : exceptionDetail.getAdditionalInfo()) {
                errorString.append(!StringUtils.isNullOrEmpty(exceptionAdditionalInfo.getProperty()) ? "\nProperty: " + exceptionAdditionalInfo.getProperty() : "")
                            .append(!StringUtils.isNullOrEmpty(exceptionAdditionalInfo.getReason()) ? "|Reason: " + exceptionAdditionalInfo.getReason() : "")
                            .append(!StringUtils.isNullOrEmpty(exceptionAdditionalInfo.getValue()) ? "|Value: " + exceptionAdditionalInfo.getValue() : "")
                            .append("\n");
            }

            errorString.append("\n");
        }

        return errorString.toString();
    }
}

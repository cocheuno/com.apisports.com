/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apisports.knime.core.exception;

/**
 * Base exception class for all API-Sports related errors.
 */
public class ApiSportsException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public ApiSportsException(String message) {
        super(message);
    }

    public ApiSportsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiSportsException(Throwable cause) {
        super(cause);
    }
}

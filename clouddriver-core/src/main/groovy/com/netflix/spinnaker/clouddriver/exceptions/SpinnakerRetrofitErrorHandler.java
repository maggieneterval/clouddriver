/*
 * Copyright 2020 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.exceptions;

import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.RetrofitError.Kind;

// todo(mneterval): move to kork
@NonnullByDefault
public final class SpinnakerRetrofitErrorHandler implements ErrorHandler {
  @Override
  public Throwable handleError(RetrofitError e) {
    if (e.getKind() == Kind.HTTP) {
      if (e.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
        return new NotFoundException(e);
      }
      return new SpinnakerHttpException(e);
    }
    return new SpinnakerNetworkException(e);
  }
}

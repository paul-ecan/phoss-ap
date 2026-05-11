/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.ap.webapp.controller;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order (1)
public class RequestBodySizeLimitFilter extends OncePerRequestFilter
{
  @Value ("${phossap.max-request-size-bytes:104857600}")
  private long m_nMaxBytes;

  @Override
  protected boolean shouldNotFilter (@NonNull final HttpServletRequest aRequest)
  {
    // Only apply to API endpoints; leave AS4 inbound and other paths unrestricted
    return !aRequest.getRequestURI ().startsWith ("/api/");
  }

  @Override
  protected void doFilterInternal (@NonNull final HttpServletRequest aRequest,
                                   @NonNull final HttpServletResponse aResponse,
                                   @NonNull final FilterChain aFilterChain) throws ServletException, IOException
  {
    final long nContentLength = aRequest.getContentLengthLong ();
    if (nContentLength > m_nMaxBytes)
    {
      aResponse.sendError (HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request body exceeds maximum allowed size");
      return;
    }
    aFilterChain.doFilter (aRequest, aResponse);
  }
}

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

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helger.base.state.ESuccess;
import com.helger.phoss.ap.core.reporting.APPeppolReportHelper;

/**
 * REST endpoint to manually trigger Peppol TSR/EUSR report generation and sending. Protected by
 * {@link ApiTokenFilter} — requires the {@code X-Token} header. Intended for test-lab use; in
 * production the monthly scheduler handles this automatically.
 */
@RestController
@RequestMapping ("/api/reporting")
public class ReportingTriggerController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ReportingTriggerController.class);

  /**
   * Trigger TSR and EUSR report generation and sending for the given month.
   *
   * @param sYearMonth
   *        Optional year-month in {@code YYYY-MM} format (e.g. {@code 2026-04}). Defaults to the
   *        previous calendar month.
   * @return 200 OK with result message, or 400 if the year-month format is invalid.
   */
  @PostMapping ("/trigger")
  public ResponseEntity <String> trigger (@RequestParam (value = "yearMonth",
                                                         required = false) final String sYearMonth)
  {
    final YearMonth aYearMonth;
    if (sYearMonth != null)
    {
      try
      {
        aYearMonth = YearMonth.parse (sYearMonth);
      }
      catch (final DateTimeParseException ex)
      {
        return ResponseEntity.badRequest ().body ("Invalid yearMonth format '" + sYearMonth + "' — expected YYYY-MM");
      }
    }
    else
    {
      aYearMonth = YearMonth.now ().minusMonths (1);
    }

    LOGGER.info ("Manual trigger: creating and sending Peppol reports for " + aYearMonth);
    final ESuccess eSuccess = APPeppolReportHelper.createAndSendPeppolReports (aYearMonth);

    if (eSuccess.isSuccess ())
      return ResponseEntity.ok ("Peppol reports triggered successfully for " + aYearMonth);

    return ResponseEntity.internalServerError ().body ("Peppol report generation failed for " + aYearMonth +
                                                       " — check server logs");
  }
}

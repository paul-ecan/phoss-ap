#!/bin/bash
#
# Copyright (C) 2026 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Send a single prebuilt SBD document to the local phoss-ap instance
JAR=$(ls target/phoss-ap-testsender-*.jar | grep -v sources | tail -1)
java -jar "$JAR" \
  --testsender.bulk.enabled=false \
  --testsender.samples.xml= \
  --testsender.samples.sbd=classpath:samples/prebuilt-sbd.xml \
  --testsender.samples.pdf= \
  "$@"

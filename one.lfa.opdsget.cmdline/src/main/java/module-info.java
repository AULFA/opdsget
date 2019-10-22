/*
 * Copyright © 2018 Library For All
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

import one.lfa.epubsquash.api.EPUBSquasherProviderType;
import one.lfa.opdsget.api.OPDSRetrieverProviderType;

/**
 * Command-line frontend.
 */

module one.lfa.opdsget.cmdline
{
  requires one.lfa.opdsget.api;
  requires one.lfa.opdsget.vanilla;
  requires one.lfa.epubsquash.api;
  requires one.lfa.epubsquash.vanilla;

  requires jcommander;
  requires org.slf4j;
  requires ch.qos.logback.classic;

  uses EPUBSquasherProviderType;
  uses OPDSRetrieverProviderType;

  exports one.lfa.opdsget.cmdline;

  opens one.lfa.opdsget.cmdline to jcommander;
}
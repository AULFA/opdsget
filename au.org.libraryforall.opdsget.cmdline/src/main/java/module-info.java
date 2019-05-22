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

import au.org.libraryforall.epubsquash.api.EPUBSquasherProviderType;
import au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType;

/**
 * Command-line frontend.
 */

module au.org.libraryforall.opdsget.cmdline
{
  requires au.org.libraryforall.opdsget.api;
  requires au.org.libraryforall.opdsget.vanilla;
  requires au.org.libraryforall.epubsquash.api;
  requires au.org.libraryforall.epubsquash.vanilla;

  requires jcommander;
  requires org.slf4j;
  requires ch.qos.logback.classic;

  uses EPUBSquasherProviderType;
  uses OPDSRetrieverProviderType;

  exports au.org.libraryforall.opdsget.cmdline;

  opens au.org.libraryforall.opdsget.cmdline to jcommander;
}
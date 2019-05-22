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

/**
 * The vanilla implementation of the {@code opdsget} API.
 */

module au.org.libraryforall.opdsget.vanilla
{
  requires java.xml;

  requires au.org.libraryforall.epubsquash.api;
  requires au.org.libraryforall.opdsget.api;
  requires org.slf4j;

  uses au.org.libraryforall.epubsquash.api.EPUBSquasherProviderType;

  exports au.org.libraryforall.opdsget.vanilla;

  provides au.org.libraryforall.opdsget.api.OPDSRetrieverProviderType
    with au.org.libraryforall.opdsget.vanilla.OPDSRetrievers;
}

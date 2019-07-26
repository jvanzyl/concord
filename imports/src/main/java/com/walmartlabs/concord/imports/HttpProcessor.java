package com.walmartlabs.concord.imports;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.dependencymanager.DependencyManager;
import com.walmartlabs.concord.repository.LastModifiedSnapshot;
import com.walmartlabs.concord.repository.Snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.walmartlabs.concord.server.queueclient.message.ImportEntry.HttpEntry;

public class HttpProcessor implements ImportProcessor<HttpEntry> {

    private final DependencyManager dependencyManager;

    public HttpProcessor(DependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    @Override
    public String type() {
        return "http";
    }

    @Override
    public Snapshot process(HttpEntry entry, Path workDir) throws Exception {
        URL url = new URL(entry.url());
        try(InputStream stream = url.openStream()) {
            Path importPath = IOUtils.createTempDir("http-import");
            Files.copy(stream, importPath, StandardCopyOption.REPLACE_EXISTING);
            return extract(entry, workDir, importPath);
        }
    }

    private Snapshot extract(HttpEntry entry, Path workDir, Path importPath) throws IOException {
        Path dest = workDir;
        if (entry.dest() != null) {
            dest = dest.resolve(entry.dest());
        }
        LastModifiedSnapshot snapshot = new LastModifiedSnapshot();
        IOUtils.copy(importPath, dest.resolve(importPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        return snapshot;
    }
}

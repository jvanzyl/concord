package com.walmartlabs.concord.server.process.pipelines.processors;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.policyengine.PolicyEngine;
import com.walmartlabs.concord.policyengine.PolicyEngineRules;
import com.walmartlabs.concord.server.cfg.DefaultProcessConfiguration;
import com.walmartlabs.concord.server.org.OrganizationDao;
import com.walmartlabs.concord.server.org.project.ProjectDao;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestDataMergingProcessorTest {

    private RequestDataMergingProcessor p;
    private ProjectDao projectDao;
    private OrganizationDao orgDao;

    @Before
    public void init() {
        projectDao = mock(ProjectDao.class);
        orgDao = mock(OrganizationDao.class);

        DefaultProcessConfiguration defaultCfg = mock(DefaultProcessConfiguration.class);
        p = new RequestDataMergingProcessor(projectDao, orgDao, defaultCfg);
    }

    @Test
    public void testAllCfg() throws Exception {
        Path workDir = Files.createTempDirectory("testAllCfg_workDir");

        UUID instanceId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID prjId = UUID.randomUUID();

        Map<String, Object> req = new HashMap<>();
        req.put("a", "a-req");
        req.put("req", "req-value");

        Map<String, Object> orgCfg = new HashMap<>();
        orgCfg.put("a", "a-org");
        orgCfg.put("org", "org-value");

        Map<String, Object> prjCfg = new HashMap<>();
        prjCfg.put("a", "a-prj");
        prjCfg.put("project", "prj-value");

        Map<String, Object> processCfgPolicy = new HashMap<>();
        processCfgPolicy.put("a", "a-process-cfg-policy");
        processCfgPolicy.put("process-cfg-policy", "process-cfg-policy-value");

        PolicyEngineRules policy = new PolicyEngineRules(null, null, null, null, null, null, null, null, processCfgPolicy);

        // ---
        when(orgDao.getConfiguration(eq(orgId))).thenReturn(orgCfg);
        when(projectDao.getConfiguration(eq(prjId))).thenReturn(prjCfg);

        Payload payload = new Payload(new ProcessKey(instanceId, new Timestamp(System.currentTimeMillis())));
        payload = payload
                .putHeader(Payload.CONFIGURATION, req)
                .putHeader(Payload.ORGANIZATION_ID, orgId)
                .putHeader(Payload.PROJECT_ID, prjId)
                .putHeader(Payload.WORKSPACE_DIR, workDir)
                .putHeader(Payload.POLICY, new PolicyEngine("test", policy));

        // ---
        Map<String, Object> expected = new HashMap<>();
        expected.put("activeProfiles", Collections.singletonList("default"));

        // orgCfg < prjCfg < req < org-policy < prj-policy
        expected.put("a", "a-process-cfg-policy");
        expected.put("org", "org-value");
        expected.put("project", "prj-value");
        expected.put("req", "req-value");
        expected.put("process-cfg-policy", "process-cfg-policy-value");

        Map<String, Object> result = process(payload);
        assertEquals(expected, result);
    }

    @Test
    public void testWithoutPolicy() throws Exception {
        Path workDir = Files.createTempDirectory("testWithoutPoliy_workDir");

        UUID instanceId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID prjId = UUID.randomUUID();

        Map<String, Object> req = new HashMap<>();
        req.put("a", "a-req");
        req.put("req", "req-value");

        Map<String, Object> orgCfg = new HashMap<>();
        orgCfg.put("a", "a-org");
        orgCfg.put("org", "org-value");

        Map<String, Object> prjCfg = new HashMap<>();
        prjCfg.put("a", "a-prj");
        prjCfg.put("project", "prj-value");

        // ---
        when(orgDao.getConfiguration(eq(orgId))).thenReturn(orgCfg);
        when(projectDao.getConfiguration(eq(prjId))).thenReturn(prjCfg);

        Payload payload = new Payload(new ProcessKey(instanceId, new Timestamp(System.currentTimeMillis())));
        payload = payload
                .putHeader(Payload.CONFIGURATION, req)
                .putHeader(Payload.ORGANIZATION_ID, orgId)
                .putHeader(Payload.PROJECT_ID, prjId)
                .putHeader(Payload.WORKSPACE_DIR, workDir);

        // ---
        Map<String, Object> expected = new HashMap<>();
        expected.put("activeProfiles", Collections.singletonList("default"));

        // orgCfg < prjCfg < req
        expected.put("a", "a-req");
        expected.put("org", "org-value");
        expected.put("project", "prj-value");
        expected.put("req", "req-value");

        Map<String, Object> result = process(payload);
        assertEquals(expected, result);
    }

    private Map<String, Object> process(Payload payload) {
        return processPayload(payload).getHeader(Payload.CONFIGURATION);
    }

    private Payload processPayload(Payload payload) {
        Chain chain = mock(Chain.class);

        when(chain.process(any())).thenAnswer((Answer<Payload>) invocation -> {
            Object[] args = invocation.getArguments();
            return (Payload) args[0];
        });

        return p.process(chain, payload);
    }
}

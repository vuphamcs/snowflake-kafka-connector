/*
 * Copyright (c) 2023 Snowflake Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.snowflake.kafka.connector.internal.streaming;

import com.snowflake.kafka.connector.SnowflakeSinkConnectorConfig;
import com.snowflake.kafka.connector.Utils;
import com.snowflake.kafka.connector.internal.TestUtils;
import java.util.Map;
import net.snowflake.ingest.streaming.SnowflakeStreamingIngestClient;
import net.snowflake.ingest.utils.SFException;
import org.apache.kafka.connect.errors.ConnectException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class StreamingClientHandlerTest {
  private StreamingClientHandler streamingClientHandler;
  private Map<String, String> connectorConfig;

  @Before
  public void setup() {
    this.streamingClientHandler = new StreamingClientHandler();
    this.connectorConfig = TestUtils.getConfForStreaming();
  }

  @Test
  public void testCreateClient() {
    SnowflakeStreamingIngestClient client =
        this.streamingClientHandler.createClient(this.connectorConfig);

    // verify valid client against config
    assert !client.isClosed();
    assert client.getName().contains(this.connectorConfig.get(Utils.NAME));
  }

  @Test
  public void testCreateClientException() {
    // invalidate the config
    this.connectorConfig.remove(Utils.SF_ROLE);

    try {
      this.streamingClientHandler.createClient(this.connectorConfig);
    } catch (ConnectException ex) {
      assert ex.getCause().getClass().equals(SFException.class);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateClientInvalidBdecVersion() {
    // add invalid bdec version
    this.connectorConfig.put(SnowflakeSinkConnectorConfig.SNOWPIPE_STREAMING_FILE_VERSION, "1");

    // test create
    this.streamingClientHandler.createClient(this.connectorConfig);
  }

  @Test
  public void testCloseClient() throws Exception {
    // setup valid client
    SnowflakeStreamingIngestClient client = Mockito.mock(SnowflakeStreamingIngestClient.class);
    Mockito.when(client.isClosed()).thenReturn(false);
    Mockito.when(client.getName()).thenReturn(this.connectorConfig.get(Utils.NAME));

    // test close
    this.streamingClientHandler.closeClient(client);

    // verify close() was called
    Mockito.verify(client, Mockito.times(1)).close();

    // these should be called in isClientValid() and logging
    Mockito.verify(client, Mockito.times(1)).isClosed();
    Mockito.verify(client, Mockito.times(2)).getName();
  }

  @Test
  public void testCloseClientException() throws Exception {
    // setup valid client
    SnowflakeStreamingIngestClient client = Mockito.mock(SnowflakeStreamingIngestClient.class);
    Mockito.when(client.isClosed()).thenReturn(false);
    Mockito.when(client.getName()).thenReturn(this.connectorConfig.get(Utils.NAME));
    Mockito.doThrow(new Exception("cant close client")).when(client).close();

    // test close
    this.streamingClientHandler.closeClient(client);

    // verify close() was called
    Mockito.verify(client, Mockito.times(1)).close();

    // these should be called in isClientValid() and logging
    Mockito.verify(client, Mockito.times(1)).isClosed();
    Mockito.verify(client, Mockito.times(2)).getName();
  }

  @Test
  public void testValidClient() {
    // valid client
    SnowflakeStreamingIngestClient validClient = Mockito.mock(SnowflakeStreamingIngestClient.class);
    Mockito.when(validClient.isClosed()).thenReturn(false);
    Mockito.when(validClient.getName()).thenReturn("testclient");
    assert StreamingClientHandler.isClientValid(validClient);
    Mockito.verify(validClient, Mockito.times(1)).isClosed();
    Mockito.verify(validClient, Mockito.times(1)).getName();
  }

  @Test
  public void testInvalidClient() {
    // invalid client - closed
    SnowflakeStreamingIngestClient closedClient =
        Mockito.mock(SnowflakeStreamingIngestClient.class);
    Mockito.when(closedClient.isClosed()).thenReturn(true);
    Mockito.when(closedClient.getName()).thenReturn("testclient");
    assert !StreamingClientHandler.isClientValid(closedClient);
    Mockito.verify(closedClient, Mockito.times(1)).isClosed();
    Mockito.verify(closedClient, Mockito.times(0)).getName();

    // invalid client - no name
    SnowflakeStreamingIngestClient unnamedClient =
        Mockito.mock(SnowflakeStreamingIngestClient.class);
    Mockito.when(unnamedClient.isClosed()).thenReturn(false);
    Mockito.when(unnamedClient.getName()).thenReturn(null);
    assert !StreamingClientHandler.isClientValid(unnamedClient);
    Mockito.verify(unnamedClient, Mockito.times(1)).isClosed();
    Mockito.verify(unnamedClient, Mockito.times(1)).getName();
  }
}

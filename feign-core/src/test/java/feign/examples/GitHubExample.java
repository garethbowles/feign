/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign.examples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import dagger.Module;
import dagger.Provides;
import feign.Feign;
import feign.codec.Decoder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * adapted from {@code com.example.retrofit.GitHubClient}
 */
public class GitHubExample {

  interface GitHub {
    @GET @Path("/repos/{owner}/{repo}/contributors") List<Contributor> contributors(
        @PathParam("owner") String owner, @PathParam("repo") String repo);
  }

  static class Contributor {
    String login;
    int contributions;
  }

  public static void main(String... args) {
    GitHub github = Feign.create(GitHub.class, "https://api.github.com", new GsonModule());

    // Fetch and print a list of the contributors to this library.
    List<Contributor> contributors = github.contributors("netflix", "feign");
    for (Contributor contributor : contributors) {
      System.out.println(contributor.login + " (" + contributor.contributions + ")");
    }
  }

  /**
   * Here's how to wire gson deserialization.
   */
  @Module(overrides = true, library = true)
  static class GsonModule {
    @Provides @Singleton Map<String, Decoder> decoders() {
      return ImmutableMap.of("GitHub", jsonDecoder);
    }

    final Decoder jsonDecoder = new Decoder() {
      Gson gson = new Gson();

      @Override public Object decode(String methodKey, Reader reader, TypeToken<?> type) {
        return gson.fromJson(reader, type.getType());
      }
    };
  }

  /**
   * Here's how to wire jackson deserialization.
   */
  @Module(overrides = true, library = true)
  static class JacksonModule {
    @Provides @Singleton Map<String, Decoder> decoders() {
      return ImmutableMap.of("GitHub", jsonDecoder);
    }

    final Decoder jsonDecoder = new Decoder() {
      ObjectMapper mapper = new ObjectMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES).setVisibility(FIELD, ANY);

      @Override public Object decode(String methodKey, Reader reader, final TypeToken<?> type)
          throws JsonProcessingException, IOException {
        return mapper.readValue(reader, mapper.constructType(type.getType()));
      }
    };
  }
}

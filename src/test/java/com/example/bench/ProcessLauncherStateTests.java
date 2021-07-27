/*
 * Copyright 2016-2017 the original author or authors.
 *
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
 */

package com.example.bench;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.example.bench.CdsBenchmark.CdsState;
import com.example.bench.CdsBenchmark.CdsState.Sample;
import com.example.reactor.ReactorApplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * @author Dave Syer
 *
 */
@ExtendWith(OutputCaptureExtension.class)
@DisabledIf("noJarInTargetDir")
public class ProcessLauncherStateTests {

	public static boolean noJarInTargetDir() {
		return !new File("target/micro-0.0.1-SNAPSHOT.jar").exists();
	}

	@Test
	public void vanilla(CapturedOutput output) throws Exception {
		// System.setProperty("bench.args",
		// "-agentlib:jdwp=transport=dt_socket,server=y,address=8000");
		System.setProperty("debug", "true");
		ProcessLauncherState state = new ProcessLauncherState("target");
		state.setMainClass(ReactorApplication.class.getName());
		state.before();
		state.run();
		state.after();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(state.getHeap()).isGreaterThan(0);
	}

	@Test
	@EnabledOnJre({ JRE.JAVA_11, JRE.JAVA_14, JRE.JAVA_17 })
	public void cds(CapturedOutput output) throws Exception {
		CdsState state = new CdsState();
		state.sample = Sample.demo;
		// state.addArgs("-Ddebug=true");
		state.start();
		state.run();
		state.after();
		assertThat(output.toString()).contains("Netty started");
		assertThat(output.toString()).contains("Benchmark app started");
	}

}

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
package com.example.graal;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(io.netty.util.internal.logging.InternalLoggerFactory.class)
final class Target_io_netty_util_internal_logging_InternalLoggerFactory {
	@Substitute
	private static InternalLoggerFactory newDefaultFactory(String name) {
		return JdkLoggerFactory.INSTANCE;
	}
}

@TargetClass(className = "io.netty.util.internal.PlatformDependent0")
final class Target_io_netty_util_internal_PlatformDependent0 {
	@Alias
	@RecomputeFieldValue(kind = Kind.FieldOffset, //
			declClassName = "java.nio.Buffer", //
			name = "address") //
	private static long ADDRESS_FIELD_OFFSET;
}

@TargetClass(className = "io.netty.util.internal.CleanerJava6")
final class Target_io_netty_util_internal_CleanerJava6 {
	@Alias
	@RecomputeFieldValue(kind = Kind.FieldOffset, //
			declClassName = "java.nio.DirectByteBuffer", //
			name = "cleaner") //
	private static long CLEANER_FIELD_OFFSET;
}

@TargetClass(className = "io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess")
final class Target_io_netty_util_internal_shaded_org_jctools_util_UnsafeRefArrayAccess {
	@Alias
	@RecomputeFieldValue(kind = Kind.ArrayIndexShift, declClass = Object[].class) //
	public static int REF_ELEMENT_SHIFT;
}

public class NettySubstitutions {
}

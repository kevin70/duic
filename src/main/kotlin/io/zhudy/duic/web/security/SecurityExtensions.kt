/**
 * Copyright 2017-2019 the original author or authors
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
package io.zhudy.duic.web.security

import io.zhudy.duic.BizCode
import io.zhudy.duic.BizCodeException
import io.zhudy.duic.UserContext
import org.springframework.web.reactive.function.server.ServerRequest


/**
 * 认证用户上下文.
 *
 * @throws BizCodeException 如果未认证将返回 [BizCode.Classic.C_401] 错误
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
fun ServerRequest.userContext(): UserContext = attribute(UserContext.CONTEXT_KEY).orElseThrow {
    BizCodeException(BizCode.Classic.C_401)
} as UserContext

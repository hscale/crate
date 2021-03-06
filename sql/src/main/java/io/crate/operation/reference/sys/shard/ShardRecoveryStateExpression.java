/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.operation.reference.sys.shard;

import io.crate.metadata.SimpleObjectExpression;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.recovery.RecoveryState;

public abstract class ShardRecoveryStateExpression<T> extends SimpleObjectExpression<T> {

    private final IndexShard indexShard;

    ShardRecoveryStateExpression(IndexShard indexShard) {
        this.indexShard = indexShard;
    }

    @Override
    public T value() {
        // the recoveryState is only null when the shard is in the initialization process where
        // this expression must not be called
        assert indexShard.recoveryState() != null : "recoveryState must not be null upon value call";
        return innerValue(indexShard.recoveryState());
    }

    protected abstract T innerValue(RecoveryState recoveryState);
}

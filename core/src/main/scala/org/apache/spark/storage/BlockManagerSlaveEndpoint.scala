/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.storage

import scala.concurrent.{ ExecutionContext, Future }

import org.apache.spark.rpc.{ RpcEnv, RpcCallContext, RpcEndpoint }
import org.apache.spark.util.ThreadUtils
import org.apache.spark.{ Logging, MapOutputTracker, SparkEnv }
import org.apache.spark.storage.BlockManagerMessages._

/**
 * An RpcEndpoint to take commands from the master to execute options. For example,
 * this is used to remove blocks from the slave's BlockManager.
 * 运行在所有Slave节点 上,接收BlockManagerMasterEndpoint的命令,例如:删除某个RDD的数据,删除某个Block
 * 删除某个Shuffle数据,返回某些Block的状态
 * Block对应RDD中提到Partition,每个Partition对应一个Block,
 * 每个Block由唯一的BlockId标示格式"rdd_"+rddId+"_"+PartitionId
 */
private[storage] class BlockManagerSlaveEndpoint(
  override val rpcEnv: RpcEnv,
  blockManager: BlockManager,//引用BlockManagerMaster与Mast消息通信
  mapOutputTracker: MapOutputTracker)
    extends RpcEndpoint with Logging {

  private val asyncThreadPool =
    ThreadUtils.newDaemonCachedThreadPool("block-manager-slave-async-thread-pool")
  private implicit val asyncExecutionContext = ExecutionContext.fromExecutorService(asyncThreadPool)

  // Operations that involve removing blocks may be slow and should be done asynchronously
  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    //根据BlockId删除该Executor上所有和该Shuffle相关的Block
    case RemoveBlock(blockId) =>
      doAsync[Boolean]("removing block " + blockId, context) {
        blockManager.removeBlock(blockId)
        true
      }     
    //收到BlockManagerMasterEndpoint发送RemoveRdd信息,根据RddId删除该Excutor上RDD所关联的所有Block
    case RemoveRdd(rddId) =>
      doAsync[Int]("removing RDD " + rddId, context) {
        blockManager.removeRdd(rddId)
      }
    //根据shuffleId删除该Executor上所有和该Shuffle相关的Block
    case RemoveShuffle(shuffleId) =>
      doAsync[Boolean]("removing shuffle " + shuffleId, context) {
        if (mapOutputTracker != null) {
          mapOutputTracker.unregisterShuffle(shuffleId)
        }
        SparkEnv.get.shuffleManager.unregisterShuffle(shuffleId)
      }
    //根据broadcastId删除该Executor上和该广播变量相关的所有Block
    case RemoveBroadcast(broadcastId, _) =>
      doAsync[Int]("removing broadcast " + broadcastId, context) {
        blockManager.removeBroadcast(broadcastId, tellMaster = true)
      }
    //根据blockId和askSlaves向Master返回该Block的blockStatus
    case GetBlockStatus(blockId, _) =>
      context.reply(blockManager.getStatus(blockId))
    //根据blockId和askSlaves向Master返回该Block的blockStatus
    case GetMatchingBlockIds(filter, _) =>
      context.reply(blockManager.getMatchingBlockIds(filter))
  }
  //科里化函数,异步调用,方法回调
  private def doAsync[T](actionMessage: String, context: RpcCallContext)(body: => T) {
    val future = Future {
      logDebug(actionMessage)
      body
    }
    future.onSuccess {
      case response =>
        logDebug("Done " + actionMessage + ", response is " + response)
        context.reply(response)
        logDebug("Sent response: " + response + " to " + context.sender)
    }
    future.onFailure {
      case t: Throwable =>
        logError("Error in " + actionMessage, t)
        context.sendFailure(t)
    }
  }

  override def onStop(): Unit = {
    asyncThreadPool.shutdownNow()
  }
}

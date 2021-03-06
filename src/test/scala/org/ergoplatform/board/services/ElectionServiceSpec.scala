package org.ergoplatform.board.services

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.ergoplatform.board.mongo.MongoFixture
import org.ergoplatform.board.protocol.{ElectionCreate, ElectionProlong}
import org.ergoplatform.board.stores.ElectionStoreImpl
import org.ergoplatform.board.{ElectionProcessorProviderHelper, FutureHelpers, Generators}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class ElectionServiceSpec extends TestKit(ActorSystem("election-spec"))
  with MongoFixture
  with Matchers
  with FutureHelpers
  with Generators
  with ElectionProcessorProviderHelper{

  it should "create, find, get, exists, prolong election" in { db =>

    val eStore = new ElectionStoreImpl(db)
    val service = new ElectionServiceImpl(eStore, electionProcessorProvider)

    val cmd = ElectionCreate(100L, 200L, Some("desc"))

    val nonExistingId = uuid

    service.find(nonExistingId).await shouldBe empty
    service.exists(nonExistingId).await shouldBe false
    the[NoSuchElementException] thrownBy service.get(nonExistingId).await
    the[NoSuchElementException] thrownBy service.extendDuration(nonExistingId, ElectionProlong(100)).await

    val created = service.create(cmd).await
    created.start shouldBe cmd.start
    created.end shouldBe cmd.end
    created.description shouldBe cmd.description

    service.find(created.id).await shouldBe Some(created)
    service.exists(created.id).await shouldBe true
    service.get(created.id).await shouldBe created
    val prolongCmd = ElectionProlong(100L)
    val prolonged = service.extendDuration(created.id, prolongCmd).await

    prolonged.end shouldEqual created.end + prolongCmd.prolongDuration
  }
}

package org.alephium.flow.network.bootstrap

import scala.util.Random

import akka.io.Tcp
import akka.testkit.TestProbe
import akka.util.ByteString

import org.alephium.flow.AlephiumFlowActorSpec
import org.alephium.flow.network.Bootstrapper
import org.alephium.protocol.model.ModelGen
import org.alephium.serde.Serde

class BrokerConnectorSpec extends AlephiumFlowActorSpec("BrokerConnector") with InfoFixture {
  it should "follow this workflow" in {
    val connection        = TestProbe()
    val cliqueCoordinator = TestProbe()
    val brokerConnector =
      system.actorOf(BrokerConnector.props(connection.ref, cliqueCoordinator.ref))
    val randomId      = Random.nextInt(config.brokerNum)
    val randomAddress = ModelGen.socketAddress.sample.get
    val randomInfo = PeerInfo.unsafe(randomId,
                                     config.groupNumPerBroker,
                                     randomAddress.getAddress,
                                     randomAddress.getPort,
                                     None,
                                     None)

    connection.expectMsgType[Tcp.Register]
    watch(brokerConnector)

    implicit val peerInfoSerde: Serde[PeerInfo] = PeerInfo._serde
    val infoData                                = BrokerConnector.envelop(randomInfo).data
    brokerConnector ! Tcp.Received(infoData)

    cliqueCoordinator.expectMsgType[PeerInfo]

    val randomCliqueInfo = genIntraCliqueInfo
    brokerConnector ! Bootstrapper.SendIntraCliqueInfo(randomCliqueInfo)
    connection.expectMsgPF() {
      case Tcp.Write(data, _) =>
        BrokerConnector.unwrap(IntraCliqueInfo._deserialize(data)) is Right(
          Some((randomCliqueInfo, ByteString.empty)))
    }

    val ackData = BrokerConnector.envelop(BrokerConnector.Ack(randomId)).data
    brokerConnector ! Tcp.Received(ackData)

    brokerConnector ! CliqueCoordinator.Ready
    connection.expectMsgPF() {
      case Tcp.Write(data, _) =>
        BrokerConnector.deserializeTry[CliqueCoordinator.Ready.type](data) is Right(
          Some((CliqueCoordinator.Ready, ByteString.empty)))
    }

    brokerConnector ! Tcp.PeerClosed

    expectTerminated(brokerConnector)
  }
}

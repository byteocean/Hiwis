//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see http://www.gnu.org/licenses/.
// 

#include "UDPAppC.h"

Define_Module(UDPAppC);

UDPAppC::UDPAppC() {
    // TODO Auto-generated constructor stub
    selfmsg = null;
}

UDPAppC::~UDPAppC() {
    // TODO Auto-generated destructor stub
    cancelAndDelete(selfmsg);
}

void UDPAppC::initialize(int stage){
    ApplicationBase::initialize(stage);

    if(stage == 0){
        numSent = 0;
        numReceived = 0;
        WATCH(numSent);
        WATCH(numReceived);
        localPort = par("localPort");
        destPort = par("destPort");
        selfmsg = new cMessage("timer");
        socket.setOutputGate(gate("udpOut"));
    }

}
void UDPAppC::finish()
{
    recordScalar("packets sent", numSent);
    recordScalar("packets received", numReceived);
    ApplicationBase::finish();
}

void UDPAppC::handleMessageWhenUp(cMessage *msg){

    if(msg->isSelfMessage()){

    }
    else{
        UDPPacket *packet_msg = check_and_cast<UDPPacket *>(msg);
        int f = packet_msg->getfunc();

        switch(f){
        case BIND: this->socket.bind(packet_msg->getsourceport);
                    break;
        case CONNECT: IPv4Address * ipv4 = new IPv4Address(packet_msg->getdestaddr);
                       this->destaddr.set(ipv4);
                       this->socket.connect(this->destaddr,packet_msg->getdestinationport);
                       break;
        case SEND:;
        }
    }
}

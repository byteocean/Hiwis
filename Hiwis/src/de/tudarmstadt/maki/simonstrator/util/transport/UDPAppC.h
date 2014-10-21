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

#ifndef UDPAPPC_H_
#define UDPAPPC_H_

#include <vector>

#include "INETDefs.h"

#include "ApplicationBase.h"
#include "UDPSocket.h"


class INET_API UDPAppC : public ApplicationBase {
protected:
    enum MsgKinds { BIND = 1,CONNECT,SEND,CLOSE };
    UDPSocket socket;
    int localPort, destPort;
    std::vector<IPvXAddress> destAddresses;
    int numSent,numReceived;
    IPvXAddress destaddr;
    cMessage *selfmsg;
public:
    UDPAppC();
    virtual ~UDPAppC();
protected:
    virtual int numInitStages() const { return 4; }
    virtual void initialize(int stage);
    virtual void handleMessageWhenUp(cMessage *msg);
    virtual void finish();

    virtual void processStart();
    virtual void processSend();
    virtual void processStop();

    virtual bool handleNodeStart(IDoneCallback *doneCallback);
    virtual bool handleNodeShutdown(IDoneCallback *doneCallback);
    virtual void handleNodeCrash();

};

#endif /* UDPAPPC_H_ */

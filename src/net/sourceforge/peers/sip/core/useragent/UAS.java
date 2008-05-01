/*
    This file is part of Peers.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008 Yohann Martineau 
*/

package net.sourceforge.peers.sip.core.useragent;

import java.io.IOException;
import java.util.ArrayList;

import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.SipServerTransportUser;
import net.sourceforge.peers.sip.transport.TransportManager;

public class UAS implements SipServerTransportUser {

    public final static ArrayList<String> SUPPORTED_METHODS;
    
    static {
        SUPPORTED_METHODS = new ArrayList<String>();
        SUPPORTED_METHODS.add(RFC3261.METHOD_INVITE);
        SUPPORTED_METHODS.add(RFC3261.METHOD_CANCEL);
    };
    
    private InitialRequestManager initialRequestManager;
    private MidDialogRequestManager midDialogRequestManager;
    
    private DialogManager dialogManager;
    
    /**
     * should be instanciated only once, it was a singleton.
     */
    public UAS(UserAgent userAgent, DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager) {
        this.dialogManager = dialogManager;
        initialRequestManager = new InitialRequestManager(userAgent,
                dialogManager, transactionManager, transportManager);
        midDialogRequestManager = new MidDialogRequestManager(userAgent,
                dialogManager, transactionManager, transportManager);
//        SipTransportFactory.getInstance().createServerTransport(this,
//                Utils.getInstance().getSipPort(), RFC3261.TRANSPORT_UDP);
        //TODO make it configurable
        try {
            transportManager.setUas(this);
            transportManager.createServerTransport(
                    RFC3261.TRANSPORT_UDP, Utils.getInstance().getSipPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void messageReceived(SipMessage sipMessage) {
        if (sipMessage instanceof SipRequest) {
            requestReceived((SipRequest) sipMessage);
        } else if (sipMessage instanceof SipResponse) {
            responseReceived((SipResponse) sipMessage);
        } else {
            throw new RuntimeException("unknown message type");
        }
    }

    private void responseReceived(SipResponse sipResponse) {
        
    }
    
    private void requestReceived(SipRequest sipRequest) {
        //TODO 8.2
        
        //TODO JTA to make request processing atomic
        
        SipHeaders headers = sipRequest.getSipHeaders();
        
        //TODO find whether the request is within an existing dialog or not
        SipHeaderFieldValue to =
            headers.get(new SipHeaderFieldName(RFC3261.HDR_TO));
        String toTag = to.getParam(new SipHeaderParamName(RFC3261.PARAM_TAG));
        if (toTag != null) {
            Dialog dialog = dialogManager.getDialog(sipRequest);
            if (dialog != null) {
                //this is a mid-dialog request
                midDialogRequestManager.manageMidDialogRequest(sipRequest, dialog);
                //TODO continue processing
            } else {
                //TODO reject the request with a 481 Call/Transaction Does Not Exist
                
            }
        } else {
            
            initialRequestManager.manageInitialRequest(sipRequest);
            
        }
    }

    public InitialRequestManager getInitialRequestManager() {
        return initialRequestManager;
    }

    public MidDialogRequestManager getMidDialogRequestManager() {
        return midDialogRequestManager;
    }
    
}

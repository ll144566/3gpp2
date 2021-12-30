package com.quectel.jnitestexec.cdma2;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

public class SmsMessage {

    private final static byte TELESERVICE_IDENTIFIER                    = 0x00;
    private final static byte SERVICE_CATEGORY                          = 0x01;
    private final static byte ORIGINATING_ADDRESS                       = 0x02;
    private final static byte ORIGINATING_SUB_ADDRESS                   = 0x03;
    private final static byte DESTINATION_ADDRESS                       = 0x04;
    private final static byte DESTINATION_SUB_ADDRESS                   = 0x05;
    private final static byte BEARER_REPLY_OPTION                       = 0x06;
    private final static byte CAUSE_CODES                               = 0x07;
    private final static byte BEARER_DATA                               = 0x08;
    private static final boolean VDBG = true;
    private static final String LOG_TAG = "SMSMessage";
    private CdmaSmsAddress mRecipientAddress;
    private CdmaSmsAddress mOriginatingAddress;
    public SmsEnvelope mEnvelope;

    public byte[] getPdu() {
        return mPdu;
    }

    private byte[] mPdu;
    private BearerData mBearerData;
    private int mMessageRef;
    private byte[] mUserData;
    private SmsHeader mUserDataHeader;
    private String mMessageBody;
    private long mScTimeMillis;
    private int status;





    public void parsePdu(byte[] pdu) {
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);
        int length;
        int bearerDataLength;
        SmsEnvelope env = new SmsEnvelope();
        CdmaSmsAddress addr = new CdmaSmsAddress();
        // We currently do not parse subaddress in PDU, but it is required when determining
        // fingerprint (see getIncomingSmsFingerprint()).
        CdmaSmsSubaddress subaddr = new CdmaSmsSubaddress();

        try {
            env.messageType = dis.readInt();
            env.teleService = dis.readInt();
            env.serviceCategory = dis.readInt();

            addr.digitMode = dis.readByte();
            addr.numberMode = dis.readByte();
            addr.ton = dis.readByte();
            addr.numberPlan = dis.readByte();

            length = dis.readUnsignedByte();
            addr.numberOfDigits = length;

            // sanity check on the length
            if (length > pdu.length) {
                throw new RuntimeException(
                        "createFromPdu: Invalid pdu, addr.numberOfDigits " + length
                                + " > pdu len " + pdu.length);
            }
            addr.origBytes = new byte[length];
            dis.read(addr.origBytes, 0, length); // digits

            env.bearerReply = dis.readInt();
            // CauseCode values:
            env.replySeqNo = dis.readByte();
            env.errorClass = dis.readByte();
            env.causeCode = dis.readByte();

            //encoded BearerData:
            bearerDataLength = dis.readInt();
            // sanity check on the length
            if (bearerDataLength > pdu.length) {
                throw new RuntimeException(
                        "createFromPdu: Invalid pdu, bearerDataLength " + bearerDataLength
                                + " > pdu len " + pdu.length);
            }
            env.bearerData = new byte[bearerDataLength];
            dis.read(env.bearerData, 0, bearerDataLength);
            dis.close();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "createFromPdu: conversion from byte array to object failed: " + ex, ex);
        }
        // link the filled objects to this SMS
        mOriginatingAddress = addr;
        env.origAddress = addr;
        env.origSubaddress = subaddr;
        mEnvelope = env;
        mPdu = pdu;
        Rlog.d("", "mPdu = " + Arrays.toString(mPdu));
        parseSms();
    }





    public void parsePduFromEfRecord(byte[] pdu) {
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);
        SmsEnvelope env = new SmsEnvelope();
        CdmaSmsAddress addr = new CdmaSmsAddress();
        CdmaSmsSubaddress subAddr = new CdmaSmsSubaddress();


        try {
            env.messageType = dis.readByte();

            while (dis.available() > 0) {
                int parameterId = dis.readByte();
                int parameterLen = dis.readUnsignedByte();
                byte[] parameterData = new byte[parameterLen];

                switch (parameterId) {
                    case TELESERVICE_IDENTIFIER:
                        /*
                         * 16 bit parameter that identifies which upper layer
                         * service access point is sending or should receive
                         * this message
                         */
                        env.teleService = dis.readUnsignedShort();
                        break;
                    case SERVICE_CATEGORY:
                        /*
                         * 16 bit parameter that identifies type of service as
                         * in 3GPP2 C.S0015-0 Table 3.4.3.2-1
                         */
                        env.serviceCategory = dis.readUnsignedShort();
                        break;
                    case ORIGINATING_ADDRESS:
                    case DESTINATION_ADDRESS:
                        dis.read(parameterData, 0, parameterLen);
                        BitwiseInputStream addrBis = new BitwiseInputStream(parameterData);
                        addr.digitMode = addrBis.read(1);
                        addr.numberMode = addrBis.read(1);
                        int numberType = 0;
                        if (addr.digitMode == CdmaSmsAddress.DIGIT_MODE_8BIT_CHAR) {
                            numberType = addrBis.read(3);
                            addr.ton = numberType;

                            if (addr.numberMode == CdmaSmsAddress.NUMBER_MODE_NOT_DATA_NETWORK)
                                addr.numberPlan = addrBis.read(4);
                        }

                        addr.numberOfDigits = addrBis.read(8);

                        byte[] data = new byte[addr.numberOfDigits];
                        byte b = 0x00;

                        if (addr.digitMode == CdmaSmsAddress.DIGIT_MODE_4BIT_DTMF) {
                            /* As per 3GPP2 C.S0005-0 Table 2.7.1.3.2.4-4 */
                            for (int index = 0; index < addr.numberOfDigits; index++) {
                                b = (byte) (0xF & addrBis.read(4));
                                // convert the value if it is 4-bit DTMF to 8
                                // bit
                                data[index] = convertDtmfToAscii(b);
                            }
                        } else if (addr.digitMode == CdmaSmsAddress.DIGIT_MODE_8BIT_CHAR) {
                            if (addr.numberMode == CdmaSmsAddress.NUMBER_MODE_NOT_DATA_NETWORK) {
                                for (int index = 0; index < addr.numberOfDigits; index++) {
                                    b = (byte) (0xFF & addrBis.read(8));
                                    data[index] = b;
                                }

                            } else if (addr.numberMode == CdmaSmsAddress.NUMBER_MODE_DATA_NETWORK) {
                                if (numberType == 2)
                                    System.out.println("TODO: Addr is email id");
                                else
                                    System.out.println("TODO: Addr is data network address");
                            } else {
                                System.out.println("Addr is of incorrect type");
                            }
                        } else {
                            System.out.println("Incorrect Digit mode");
                        }
                        addr.origBytes = data;
                        System.out.println("Addr=" + addr.toString());
                        mOriginatingAddress = addr;
                        if (parameterId == DESTINATION_ADDRESS) {
                            // Original address awlays indicates one sender's address for 3GPP2
                            // Here add recipient address support along with 3GPP
                            env.destAddress = addr;
                            mRecipientAddress = addr;
                        }
                        break;
                    case ORIGINATING_SUB_ADDRESS:
                    case DESTINATION_SUB_ADDRESS:
                        dis.read(parameterData, 0, parameterLen);
                        BitwiseInputStream subAddrBis = new BitwiseInputStream(parameterData);
                        subAddr.type = subAddrBis.read(3);
                        subAddr.odd = subAddrBis.readByteArray(1)[0];
                        int subAddrLen = subAddrBis.read(8);
                        byte[] subdata = new byte[subAddrLen];
                        for (int index = 0; index < subAddrLen; index++) {
                            b = (byte) (0xFF & subAddrBis.read(4));
                            // convert the value if it is 4-bit DTMF to 8 bit
                            subdata[index] = convertDtmfToAscii(b);
                        }
                        subAddr.origBytes = subdata;
                        break;
                    case BEARER_REPLY_OPTION:
                        dis.read(parameterData, 0, parameterLen);
                        BitwiseInputStream replyOptBis = new BitwiseInputStream(parameterData);
                        env.bearerReply = replyOptBis.read(6);
                        break;
                    case CAUSE_CODES:
                        dis.read(parameterData, 0, parameterLen);
                        BitwiseInputStream ccBis = new BitwiseInputStream(parameterData);
                        env.replySeqNo = ccBis.readByteArray(6)[0];
                        env.errorClass = ccBis.readByteArray(2)[0];
                        if (env.errorClass != 0x00)
                            env.causeCode = ccBis.readByteArray(8)[0];
                        break;
                    case BEARER_DATA:
                        dis.read(parameterData, 0, parameterLen);
                        env.bearerData = parameterData;
                        break;
                    default:
                        throw new Exception("unsupported parameterId (" + parameterId + ")");
                }
            }
            bais.close();
            dis.close();
        } catch (Exception ex) {
        }

        // link the filled objects to this SMS
        mOriginatingAddress = addr;
        env.origAddress = addr;
        env.origSubaddress = subAddr;
        mEnvelope = env;
        mPdu = pdu;

        parseSms();
    }

    public void parseSms() {
        // Message Waiting Info Record defined in 3GPP2 C.S-0005, 3.7.5.6
        // It contains only an 8-bit number with the number of messages waiting
        if (mEnvelope.teleService == SmsEnvelope.TELESERVICE_MWI) {
            mBearerData = new BearerData();
            if (mEnvelope.bearerData != null) {
                mBearerData.numberOfMessages = 0x000000FF & mEnvelope.bearerData[0];
            }
            if (VDBG) {
                Rlog.d(LOG_TAG, "parseSms: get MWI " +
                        Integer.toString(mBearerData.numberOfMessages));
            }
            return;
        }
        mBearerData = BearerData.decode(mEnvelope.bearerData);
        if (true) {
            Rlog.d(LOG_TAG, "MT raw BearerData = '");
            Rlog.d(LOG_TAG, "MT (decoded) BearerData = " + mBearerData);
        }
        mMessageRef = mBearerData.messageId;
        if (mBearerData.userData != null) {
            mUserData = mBearerData.userData.payload;
            mUserDataHeader = mBearerData.userData.userDataHeader;
            mMessageBody = mBearerData.userData.payloadStr;
            mPdu = mBearerData.userData.payload;
        }

        if (mOriginatingAddress != null) {
            decodeSmsDisplayAddress(mOriginatingAddress);
            if (VDBG) Rlog.v(LOG_TAG, "SMS originating address: "
                    + mOriginatingAddress.address);
        }

        if (mRecipientAddress != null) {
            decodeSmsDisplayAddress(mRecipientAddress);
        }

        if (mBearerData.msgCenterTimeStamp != null) {
            mScTimeMillis = mBearerData.msgCenterTimeStamp.toMillis(true);
        }

        if (VDBG) Rlog.d(LOG_TAG, "SMS SC timestamp: " + mScTimeMillis);

        // Message Type (See 3GPP2 C.S0015-B, v2, 4.5.1)
        if (mBearerData.messageType == BearerData.MESSAGE_TYPE_DELIVERY_ACK) {
            // The BearerData MsgStatus subparameter should only be
            // included for DELIVERY_ACK messages.  If it occurred for
            // other messages, it would be unclear what the status
            // being reported refers to.  The MsgStatus subparameter
            // is primarily useful to indicate error conditions -- a
            // message without this subparameter is assumed to
            // indicate successful delivery (status == 0).
            if (! mBearerData.messageStatusSet) {
                Rlog.d(LOG_TAG, "DELIVERY_ACK message without msgStatus (" +
                        (mUserData == null ? "also missing" : "does have") +
                        " userData).");
                status = 0;
            } else {
                status = mBearerData.errorClass << 8;
                status |= mBearerData.messageStatus;
            }
        } else if (mBearerData.messageType != BearerData.MESSAGE_TYPE_DELIVER
                && mBearerData.messageType != BearerData.MESSAGE_TYPE_SUBMIT) {
            throw new RuntimeException("Unsupported message type: " + mBearerData.messageType);
        }

        if (mMessageBody != null) {
            StringBuilder builder = new StringBuilder();
            for (byte aByte : mMessageBody.getBytes()) {
                builder.append(aByte);
            }
            if (VDBG) Rlog.v("1008689123", "SMS message body: '" +builder + "'");
            parseMessageBody();
        } else if ((mUserData != null) && VDBG) {
            Rlog.v(LOG_TAG, "SMS payload: '" + IccUtils.bytesToHexString(mUserData) + "'");
        }
    }

    private void decodeSmsDisplayAddress(SmsAddress addr) {
        addr.address = new String(addr.origBytes);
        if (addr.ton == CdmaSmsAddress.TON_INTERNATIONAL_OR_IP) {
            if (addr.address.charAt(0) != '+') {
                addr.address = "+" + addr.address;
            }
        }
        Rlog.pii(LOG_TAG, " decodeSmsDisplayAddress = " + addr.address);
    }
    protected void parseMessageBody() {
        // originatingAddress could be null if this message is from a status
        // report.
        if (mOriginatingAddress != null && mOriginatingAddress.couldBeEmailGateway()) {
            extractEmailAddressFromMessageBody();
        }
    }

    protected void extractEmailAddressFromMessageBody() {

        /* Some carriers may use " /" delimiter as below
         *
         * 1. [x@y][ ]/[subject][ ]/[body]
         * -or-
         * 2. [x@y][ ]/[body]
         */
        String[] parts = mMessageBody.split("( /)|( )", 2);
        if (parts.length < 2) return;
        /*mEmailFrom = parts[0];
        mEmailBody = parts[1];
        mIsEmail = Telephony.Mms.isEmailAddress(mEmailFrom);*/
    }

    public static byte convertDtmfToAscii(byte dtmfDigit) {
        byte asciiDigit;

        switch (dtmfDigit) {
            case  0: asciiDigit = 68; break; // 'D'
            case  1: asciiDigit = 49; break; // '1'
            case  2: asciiDigit = 50; break; // '2'
            case  3: asciiDigit = 51; break; // '3'
            case  4: asciiDigit = 52; break; // '4'
            case  5: asciiDigit = 53; break; // '5'
            case  6: asciiDigit = 54; break; // '6'
            case  7: asciiDigit = 55; break; // '7'
            case  8: asciiDigit = 56; break; // '8'
            case  9: asciiDigit = 57; break; // '9'
            case 10: asciiDigit = 48; break; // '0'
            case 11: asciiDigit = 42; break; // '*'
            case 12: asciiDigit = 35; break; // '#'
            case 13: asciiDigit = 65; break; // 'A'
            case 14: asciiDigit = 66; break; // 'B'
            case 15: asciiDigit = 67; break; // 'C'
            default:
                asciiDigit = 32; // Invalid DTMF code
                break;
        }

        return asciiDigit;
    }
}

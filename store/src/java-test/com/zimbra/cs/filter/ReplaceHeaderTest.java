/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import static org.junit.Assert.fail;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Header;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;
import com.zimbra.cs.service.util.ItemId;

public class ReplaceHeaderTest {

    private static String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: test1\n"
            + "X-Test-Header: test2\n"
            + "X-Test-Header: test3\n"
            + "X-Numeric-Header: 2\n"
            + "X-Numeric-Header: 3\n"
            + "X-Numeric-Header: 4\n"
            + "X-Spam-Score: 85\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";
    private static String sampleBaseMsg2 = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: line1\n"
            + "\tline2\n"
            + "\tline3\n"
            + "X-Spam-Score: 85\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";
    private static String sampleBaseMsg3 = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: =?utf-8?B?W1NQQU1d5pel5pys6Kqe44Gu5Lu25ZCN?=\n"
            + "X-Test-Header: ABC\n"
            + "X-Test-Header: 123\n"
            + "X-Test-Header: abc\n"
            + "X-Test-Header: \"\"\n"
            + "X-Test-Header: XYZ\n"
            + "X-Test-Header: \n"
            + "X-Test-Header: xyz\n"
            + "X-Test-Header: ''\n"
            + "X-Spam-Score: 85\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";
    private static String sampleBaseMsg4 = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: =?utf-8?B?W1NQQU1d5pel5\r\n\tpys6Kqe44Gu5Lu25ZCN?=\n"
            + "X-Spam-Score: 85\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";
    private static String sampleBaseMsg5 = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: =?utf-8?B?44GT44KM44Gv6KSH5pWw6KGM44Gr5rih44KL?=\n"
            + "\t=?utf-8?B?5Lu25ZCN44Gn44GZ44CC5Lu25ZCN44Gv44Kt?=\n"
            + "\t=?utf-8?B?44Oj44Op44Kv44K/44O844K744OD44OI44GM?=\n"
            + "\t=?utf-8?B?44Om44O844OG44Kj44O844Ko44OV44Ko44Kk?=\n"
            + "\t=?utf-8?B?44OI44Gn44CB44OZ44O844K55YWt5Y2B5Zub?=\n"
            + "\t=?utf-8?B?44Gn44Ko44Oz44Kz44O844OJ44GV44KM44G+?=\n"
            + "\t=?utf-8?B?44GZ44CC6KGM44GM6ZW344GE44Gu44Gn44CB?=\n"
            + "\t=?utf-8?B?5oqY44KK5puy44GS44KJ44KM44G+44GZ44CC?=\n"
            + "X-Spam-Score: 85\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";
    private static String sampleBaseMsg6 = "Subject: =?utf-8?B?44GT44KM44Gv6KSH5pWw6KGM44Gr5rih44KL?=\n"
            + "\t=?utf-8?B?5Lu25ZCN44Gn44GZ44CC5Lu25ZCN44Gv44Kt?=\n"
            + "\t=?utf-8?B?44Oj44Op44Kv44K/44O844K744OD44OI44GM?=\n"
            + "\t=?utf-8?B?44Om44O844OG44Kj44O844Ko44OV44Ko44Kk?=\n"
            + "\t=?utf-8?B?44OI44Gn44CB44OZ44O844K55YWt5Y2B5Zub?=\n"
            + "\t=?utf-8?B?44Gn44Ko44Oz44Kz44O844OJ44GV44KM44G+?=\n"
            + "\t=?utf-8?B?44GZ44CC6KGM44GM6ZW344GE44Gu44Gn44CB?=\n"
            + "\t=?utf-8?B?5oqY44KK5puy44GS44KJ44KM44G+44GZ44CC?=\n"
            + "from: test2@zimbra.com\n"
            + "to: test@zimbra.com\n";
    private static String[] sampleBaseMsg7 = {
              "Received: from domain1.zimbra.com (LHLO zcs-ubuntu.local) (192.168.44.131)\r\n"
            + " by zcs-ubuntu.local with LMTP; Wed, 7 Dec 2016 15:10:58 +0900 (JST)",
              "Received: from zcs-ubuntu.local (localhost [127.0.0.1])\r\n"
            + " by zcs-ubuntu.local (Postf ix) with ESMTPS id 3D4EA2C2648\r\n"
            + " for <user1@domain1.zimbra.com>; Wed,  7 Dec 2016 15:10:58 +0900 (JST)",
              "Received: from zcs-ubuntu.local (localhost [127.0.0.1])\r\n"
            + " by zcs-ubuntu.local (Postfix) with ESMTPS id 328882C26FC\r\n"
            + " for <user1@domain1.zimbra.com>; Wed,  7 Dec 2016 15:10:58 +0900 (JST)",
              "Received: from zcs-ubuntu.local (localhost [127.0.0.1])\r\n"
            + " by zcs-ubuntu.local (Postfix) with ESMTPS id 2822F2C2648\r\n"
            + " for <user1@domain1.zimbra.com>; Wed,  7 Dec 2016 15:10:58 +0900 (JST)",
              "Message-ID: <46941357.16.1482318459470.JavaMail.zimbra@dev07>",
              "MIME-Version: 1.0",
              "Content-Transfer-Encoding: 7bit",
              "X-Dummy-Header: ABC",
              "X-Dummy-Header: 123",
              "X-Dummy-Header: abc",
              "X-Dummy-Header: \"\"",
              "X-Dummy-Header: this is sample",
              "X-Dummy-Header: ",
              "X-Dummy-Header: test",
              "X-Dummy-Header: ''",
              "X-Dummy-Header: a1b2c3"};
    private static String sampleBaseMsg8 = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: test1\n"
            + "X-Test-Header: test2\n"
            + "X-Test-Header: test3\n"
            + "X-Test-Header: test4\n"
            + "X-Test-Header: test5\n"
            + "X-Test-Header: test6\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("test@zimbra.com", "secret", attrs);
        Server server = Provisioning.getInstance().getServer(acct);
        server.setSieveFeatureVariablesEnabled(true);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test3@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());

    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    /*
     * Replace subject
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeader() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newvalue \"my subject\" :contains \"Subject\" \"example\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String newSubject = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header temp = enumeration.nextElement();
                if ("Subject".equals(temp.getName())) {
                    newSubject = temp.getValue();
                    break;
                }
            }
            Assert.assertEquals("my subject", newSubject);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header value at index
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderAtIndex() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :index 2 :newvalue \"new test\" :contains \"X-Test-Header\" \"test\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    indexMatch++;
                    if (indexMatch == 2) {
                        Assert.assertEquals("new test", header.getValue());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace last header value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceLastHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :last :newvalue \"new test\" :contains \"X-Test-Header\" \"test\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    indexMatch++;
                    if (indexMatch == 3) {
                        Assert.assertEquals("new test", header.getValue());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header value of 2nd from bottom
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceSecondFromBottomHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :index 2 :last :newvalue \"new test\" :contains \"X-Test-Header\" \"test\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    indexMatch++;
                    if (indexMatch == 2) {
                        Assert.assertEquals("new test", header.getValue());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header when no value-patterns are specified
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceNoValuePattern() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"X-New-Header\" :newvalue \"new value\" :comparator \"i;ascii-casemap\" :matches \"Subject\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage()
                .getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-New-Header".equals(header.getName())) {
                    Assert.assertEquals("new value", header.getValue());
                } else if ("Subject".equals(header.getName())) {
                    fail("Subject header should have been replaced");
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }


    /*
     * Replace subject using is match-type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceSubjectHeaderUsingIs() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newvalue \"new test\" :is \"Subject\" \"example\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("Subject".equals(header.getName())) {
                    Assert.assertEquals("new test", header.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace subject using matches match-type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceSubjectHeaderUsingMatches() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newvalue \"new test\" :matches \"Subject\" \"ex*\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("Subject".equals(header.getName())) {
                    Assert.assertEquals("new test", header.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace name of the header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceNameOfHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"X-Test2-Header\" :contains \"X-Test-Header\" \"test1\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test2-Header".equals(header.getName())) {
                    Assert.assertEquals("test1", header.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header with numeric comparator :value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderWithNumericComparisionUsingValue() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"X-Numeric2-Header\" :newvalue \"0\" :value \"lt\" :comparator \"i;ascii-numeric\" \"X-Numeric-Header\" \"3\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric2-Header".equals(header.getName())) {
                    Assert.assertEquals("0", header.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header with numeric comparator :count
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderWithNumericComparisionUsingCount() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"X-Numeric2-Header\" :count \"ge\" :comparator \"i;ascii-numeric\" \"X-Numeric-Header\" \"3\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            int headerCount = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric2-Header".equals(header.getName())) {
                    headerCount++;
                }
            }
            Assert.assertEquals("3", String.valueOf(headerCount));
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header with numeric comparator :count
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderWithXSpamScore() {
        try {
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                    + "if anyof(header :value \"ge\" :comparator \"i;ascii-numeric\" [\"X-Spam-Score\"] [\"80\"]) {"
                    +"      if exists \"Subject\" {"
                    +"        replaceheader :newvalue \"[SPAM]${1}\" :matches \"Subject\" \"*\";"
                    +"      } else {"
                    +"        addheader :last \"Subject\" \"[SPAM]\";"
                    +"      }"
                    +"    }";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String subjectValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("Subject".equals(header.getName())) {
                    subjectValue = header.getValue();
                }
            }
            Assert.assertEquals("[SPAM]example", subjectValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header with multiline valued header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderForMultilineValuedHeader() {
        try {
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                    +"        replaceheader :newvalue \"${1}[test]${2}\" :matches \"X-Test-Header\" \"*line2*\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg2.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String headerValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    headerValue = header.getValue();
                    break;
                }
            }
            Assert.assertEquals("line1 [test] line3", headerValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace encoded header value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderForEncodedHeaderValue() {
        try {
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                    + "replaceheader :newvalue \"[test]${1}\" :matches \"X-Test-Header\" \"*\";"
                    + "tag \"tag1-${1}\";"
                    + "tag \"tag2-${2}\";"
                    + "tag \"tag3-${3}\";"
                    + "tag \"tag4-${4}\";"
                    + "tag \"tag5-${5}\";"
                    + "tag \"tag6-${6}\";"
                    + "tag \"tag7-${7}\";"
                    + "tag \"tag8-${8}\";"
                    + "tag \"tag9-${9}\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg3.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String headerValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    headerValue = header.getValue();
                    break;
                }
            }
            String matchValue = "=?UTF-8?Q?[test][SP?=\r\n"
                    + " =?UTF-8?Q?AM]=E6=97=A5=E6=9C=AC=E8=AA=9E=E3=81=AE=E4=BB=B6=E5=90=8D?=";
            Assert.assertEquals(matchValue, headerValue);

            String expectedTags[] = {
                    "tag1-[SPAM]日本語の件名",
                    "tag2-ABC",
                    "tag3-123",
                    "tag4-abc",
                    "tag5-\"\"",
                    "tag6-XYZ",
                    "tag7-",
                    "tag8-xyz",
                    "tag9-\'\'"};
            String resultTags[] = message.getTags();
            for (String resultTag : resultTags) {
                String expectedTag = null;
                for (String testTag : expectedTags) {
                    if (testTag.equalsIgnoreCase(resultTag)) {
                        expectedTag = testTag;
                        break;
                    }
                }
                Assert.assertEquals(expectedTag, resultTag);
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace invalid encoded multiline header value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderForInvalidEncodedMultilineHeaderValue() {
        try {
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                    +"        replaceheader :newvalue \"[test]${1}\" :matches \"X-Test-Header\" \"*\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg4.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String headerValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    headerValue = header.getValue();
                    break;
                }
            }
            Assert.assertEquals("[test]=?utf-8?B?W1NQQU1d5pel5 pys6Kqe44Gu5Lu25ZCN?=", headerValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace valid encoded multiline header value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderForValidEncodedMultilineHeaderValue() {
        try {
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                    +"        replaceheader :newvalue \"[test]${1}\" :matches \"X-Test-Header\" \"*\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg5.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String headerValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    headerValue = header.getValue();
                    break;
                }
            }
            String matchValue = "=?UTF-8?B?W3Rlc3Rd44GT44KM44Gv6KSH5pWw6KGM44Gr5rih44KL5Lu25ZCN44Gn44GZ?=\r\n"
                    + " =?UTF-8?B?44CC5Lu25ZCN44Gv44Kt44Oj44Op44Kv44K/44O8?=\r\n"
                    + " =?UTF-8?B?44K744OD44OI44GM44Om44O844OG44Kj44O844Ko?=\r\n"
                    + " =?UTF-8?B?44OV44Ko44Kk44OI44Gn44CB44OZ44O844K5?=\r\n"
                    + " =?UTF-8?B?5YWt5Y2B5Zub44Gn44Ko44Oz44Kz44O844OJ44GV?=\r\n"
                    + " =?UTF-8?B?44KM44G+44GZ44CC6KGM44GM6ZW344GE44Gu44Gn?=\r\n"
                    + " =?UTF-8?B?44CB5oqY44KK5puy44GS44KJ44KM44G+44GZ44CC?=";
            Assert.assertEquals(matchValue, headerValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace the subject with non-ASCII strings
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderNonAscii() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + "replaceheader :newvalue \"[追加]${1}\" :matches \"Subject\" \"*\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String newSubject = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header temp = enumeration.nextElement();
                if ("Subject".equals(temp.getName())) {
                    newSubject = temp.getValue();
                    break;
                }
            }
            Assert.assertEquals("=?UTF-8?Q?[=E8=BF=BD=E5=8A=A0]example?=", newSubject);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace the variable Subject with non-ASCII strings
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderNonAsciiVariables() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + "replaceheader :newvalue \"[追加]${1}\" :matches \"Subject\" \"これは複数行に渡る*\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg6.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String newSubject = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header temp = enumeration.nextElement();
                if ("Subject".equals(temp.getName())) {
                    newSubject = temp.getValue();
                    break;
                }
            }
            Assert.assertEquals("=?UTF-8?B?W+i/veWKoF3ku7blkI3jgafjgZnjgILku7blkI3jga/jgq3jg6Pjg6njgq8=?=\r\n"
                    + " =?UTF-8?B?44K/44O844K744OD44OI44GM44Om44O8?=\r\n"
                    + " =?UTF-8?B?44OG44Kj44O844Ko44OV44Ko44Kk44OI44Gn?=\r\n"
                    + " =?UTF-8?B?44CB44OZ44O844K55YWt5Y2B5Zub44Gn?=\r\n"
                    + " =?UTF-8?B?44Ko44Oz44Kz44O844OJ44GV44KM44G+44GZ?=\r\n"
                    + " =?UTF-8?B?44CC6KGM44GM6ZW344GE44Gu44Gn44CB?=\r\n"
                    + " =?UTF-8?B?5oqY44KK5puy44GS44KJ44KM44G+44GZ44CC?=", newSubject);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace the headers with "X-Dummy-Header: ABC"
     * Verify that the order of the header fields does not change.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderSameHeaderNameMultipleHeaderValues1() {
        StringBuffer triggeringMsg = new StringBuffer();
        for (String line : sampleBaseMsg7) {
            triggeringMsg.append(line).append("\r\n");
        }
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "replaceheader :newname \"X-New-Header\" :newvalue \"new value\" "
                    + ":comparator \"i;ascii-casemap\" :is \"X-Dummy-Header\" \"ABC\";";
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(triggeringMsg.toString().getBytes(), false), 0,
                    account.getName(), null, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            int index = 0;
            for (Enumeration<Header> e = msg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                String header = temp.getName();
                String value = header + ": " + temp.getValue();
                if ("X-New-Header".equals(header)) {
                    Assert.assertEquals("X-New-Header: new value", value);
                } else {
                    Assert.assertEquals(sampleBaseMsg7[index], value);
                }
                index++;
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace the "X-Dummy-Header" headers with an empty header value.
     * Verify that the order of the header fields does not change.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderSameHeaderNameMultipleHeaderValues2() {
        StringBuffer triggeringMsg = new StringBuffer();
        for (String line : sampleBaseMsg7) {
            triggeringMsg.append(line).append("\r\n");
        }
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "replaceheader :newname \"X-New-Header\" :newvalue \"new value\" "
                    + ":comparator \"i;ascii-casemap\" :is \"X-Dummy-Header\" \"\";";
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(triggeringMsg.toString().getBytes(), false), 0,
                    account.getName(), null, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            int index = 0;
            for (Enumeration<Header> e = msg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                String header = temp.getName();
                String value = header + ": " + temp.getValue();
                if ("X-New-Header".equals(header)) {
                    Assert.assertEquals("X-New-Header: new value", value);
                } else {
                    Assert.assertEquals(sampleBaseMsg7[index], value);
                }
                index++;
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header value of 2nd from bottom
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderFromBottom() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :index 1 :last :newvalue \"new test1\" :contains \"X-Test-Header\" \"test\";\n"
                    + " replaceheader :index 2 :last :newvalue \"new test2\" :contains \"X-Test-Header\" \"test\";\n"
                    + " replaceheader :index 3 :last :newvalue \"new test3\" :contains \"X-Test-Header\" \"test\";\n"
                    + " replaceheader :index 4 :last :newvalue \"new test4\" :contains \"X-Test-Header\" \"test\";\n"
                    + " replaceheader :index 5 :last :newvalue \"new test5\" :contains \"X-Test-Header\" \"test\";\n"
                    + " replaceheader :index 6 :last :newvalue \"new test6\" :contains \"X-Test-Header\" \"test\";\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg8.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 6;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    Assert.assertEquals("new test"+indexMatch, header.getValue());
                    indexMatch--;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header with ascii-casemap comparator and :value match type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderWithCaseMapComparatorUsingValue() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"X-Test2-Header\" :newvalue \"0\" :value \"lt\" :comparator \"i;ascii-casemap\" \"X-Test-Header\" \"test2\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String match = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test2-Header".equals(header.getName())) {
                	match = header.getValue();
                    break;
                }
            }
            Assert.assertEquals("0", match);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header without match-type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderWithoutMatchType() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"X-Test2-Header\" :newvalue \"0\" \"X-Test-Header\" \"test2\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String match = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test2-Header".equals(header.getName())) {
                    match = header.getValue();
                    break;
                }
            }
            Assert.assertEquals("0", match);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Replace header with empty new name should not replace the original header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceHeaderWithEmptyHeaderNewName() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " replaceheader :newname \"\" :newvalue \"0\" :value \"lt\" :comparator \"i;ascii-casemap\" \"X-Test-Header\" \"test2\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            boolean found = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                Assert.assertFalse(header.getName().equals(""));
                if (header.getName().equals("X-Test-Header") && header.getValue().equals("test2")) {
                    found = true;
                }
            }
            Assert.assertTrue(found);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }
}
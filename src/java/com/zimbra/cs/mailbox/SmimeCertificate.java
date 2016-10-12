/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.ParsedDocument;

public class SmimeCertificate extends Document{

    public SmimeCertificate(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        super(mbox, data, skipCache);
    }

    static SmimeCertificate create(int id, String uuid, Folder folder, String filename, String type, ParsedDocument pd,
            CustomMetadata custom, int flags, MailItem parent) throws ServiceException {
        assert(id != Mailbox.ID_AUTO_INCREMENT);

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = prepareCreate(Type.SMIME_CERTIFICATE, id, uuid, folder, filename, type, pd, null, custom, flags);
        if (parent != null) {
            data.parentId = parent.mId;
        }

        data.contentChanged(mbox);

        ZimbraLog.mailop.info("Adding Smime Certificate %s: id=%d, folderId=%d, folderName=%s",
                filename, data.id, folder.getId(), folder.getName());
        new DbMailItem(mbox).create(data);

        SmimeCertificate doc = new SmimeCertificate(mbox, data, false);
        doc.finishCreation(parent);
        pd.setVersion(doc.getVersion());
        return doc;
    }


}

/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://developer.berlios.de/projects/izpack/
 *
 * Copyright 2008 Jeff Gordon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.userinput.validator;

import java.security.SecureRandom;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import com.izforge.izpack.panels.userinput.PasswordGroup;
import com.izforge.izpack.panels.userinput.processorclient.ProcessingClient;
import com.izforge.izpack.util.Base64;

/**
 * @author Jeff Gordon
 */
public class PasswordEncryptionValidator implements Validator
{
    private static final Logger logger = Logger.getLogger(PasswordEncryptionValidator.class.getName());

    private Cipher encryptCipher;

    @Override
    public boolean validate(ProcessingClient client)
    {
        boolean returnValue = true;
        String encryptedPassword = null;
        String key = null;
        String algorithm = null;
        Map<String, String> params = getParams(client);
        try
        {
            key = params.get("encryptionKey");
            algorithm = params.get("algorithm");
            if (key != null && algorithm != null)
            {
                initialize(key, algorithm);
                encryptedPassword = encryptString(client.getFieldContents(0));
                if (encryptedPassword != null)
                {
                    PasswordGroup group = (PasswordGroup) client;
                    group.setModifiedPassword(encryptedPassword);
                }
                else
                {
                    returnValue = false;
                }
            }
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Password encryption failed: " + e, e);
            returnValue = false;
        }
        return (returnValue);
    }

    private Map<String, String> getParams(ProcessingClient client)
    {
        PasswordGroup group = null;
        Map<String, String> params = null;
        try
        {
            group = (PasswordGroup) client;
            if (group.hasParams())
            {
                params = group.getValidatorParams();
            }
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Getting validator parameters failed: " + e, e);
        }
        return params;
    }

    private void initialize(String key, String algorithm) throws Exception
    {
        try
        {
            //Generate the key bytes
            KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
            keygen.init(new SecureRandom(key.getBytes()));
            byte[] keyBytes = keygen.generateKey().getEncoded();
            SecretKeySpec specKey = new SecretKeySpec(keyBytes, algorithm);
            //Initialize the encryption cipher
            encryptCipher = Cipher.getInstance(algorithm);
            encryptCipher.init(Cipher.ENCRYPT_MODE, specKey);
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Error initializing password encryption: " + e, e);
            throw e;
        }
    }

    public String encryptString(String string) throws Exception
    {
        String result = null;
        try
        {
            byte[] cryptedbytes = null;
            cryptedbytes = encryptCipher.doFinal(string.getBytes("UTF-8"));
            result = Base64.encodeBytes(cryptedbytes);
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Error encrypting string: " + e, e);
            throw e;
        }
        return result;
    }
}

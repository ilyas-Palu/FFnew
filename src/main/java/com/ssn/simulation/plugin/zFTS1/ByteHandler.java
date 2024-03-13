package com.ssn.simulation.plugin.zFTS1;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteHandler {
    protected byte fillByte;
    protected int telegramLength;
    protected byte[] terminator;

    public ByteHandler(byte fillByte, int telegramLength, byte[] terminator) {
        this.fillByte = fillByte;
        this.telegramLength = telegramLength;
        this.terminator = terminator;
    }

    public int getTelegramLength() {
        return this.telegramLength;
    }

    public <T> T read(byte[] data, Class<T> clazz) throws ByteReadException {
        try {
            T obj;
            try {
                obj = clazz.getDeclaredConstructor(ByteHandler.class).newInstance(this);
            } catch (NoSuchMethodException e) {
                obj = clazz.getDeclaredConstructor(new Class<?>[0]).newInstance();
            }
            this.read(data, obj);
            return obj;
        } catch (ByteReadException e) {
            throw e;
        } catch (Exception e) {
            throw new ByteReadException(e.getMessage());
        }
    }

    public void read(byte[] data, Object target) throws ByteReadException {
        try {
            for (var attr : target.getClass().getDeclaredFields()) {
                var field = attr.getAnnotation(TelegramField.class);
                if (field == null) {
                    continue;
                }
                attr.setAccessible(true);
                var attrType = attr.getType();
                if (attrType == String.class) {
                    String val = new String(data, field.offset(), field.length(), StandardCharsets.US_ASCII);
                    attr.set(target, val.replace((char) this.fillByte, ' ').stripTrailing());
                } else if (attrType == int.class) {
                    int val = 0;
                    for (int i = 0; i < field.length(); ++i) {
                        var digit = data[field.offset() + i];
                        if (digit < 0x30 || digit > 0x39) {
                            throw new ByteReadException("character \"" + (char) digit + "\" on position "
                                    + (field.offset() + i) + " is not a digit");
                        }
                        val = (val * 10) + (digit - 0x30);
                    }
                    attr.set(target, val);
                } else if (attrType == boolean.class) {
                    attr.set(target, data[field.offset()] == (byte) 'X');
                } else {
                    throw new ByteReadException(
                            "invalid type of field " + attr.getName() + " of class " + target.getClass().getName());
                }
            }
        } catch (ByteReadException e) {
            throw e;
        } catch (Exception e) {
            throw new ByteReadException(e.getMessage());
        }
    }

    public void write(Object obj, byte[] target) throws ByteWriteException {
        this.write(obj, obj.getClass(), target);
    }

    public void write(Object obj, Class<?> clazz, byte[] target) throws ByteWriteException {
        try {
            for (var attr : clazz.getDeclaredFields()) {
                attr.setAccessible(true);
                var field = attr.getDeclaredAnnotation(TelegramField.class);
                if (field == null) {
                    continue;
                }
                var value = attr.get(obj);
                var attrType = attr.getType();
                if (attrType == String.class) {
                    Arrays.fill(target, field.offset(), field.offset() + field.length() - 1, this.fillByte);
                    if (value != null) {
                        var bytes = ((String) value).getBytes(StandardCharsets.US_ASCII);
                        System.arraycopy(bytes, 0, target, field.offset(), Math.min(field.length(), bytes.length));
                    }
                } else if (attrType == int.class) {
                    var val = (int) value;
                    for (int i = field.offset() + field.length() - 1; i >= field.offset(); --i) {
                        target[i] = (byte) (((val % 10) & 0xff) + 0x30);
                        val /= 10;
                    }
                } else if (attrType == boolean.class) {
                    if ((boolean) value) {
                        target[field.offset()] = (byte) 'X';
                    }
                }
            }
        } catch (Exception e) {
            throw new ByteWriteException(e.getMessage());
        }
    }

    public byte[] createTelegram() {
        var tele = new byte[this.telegramLength];
        Arrays.fill(tele, this.fillByte);
        System.arraycopy(this.terminator, 0, tele, tele.length - this.terminator.length, this.terminator.length);
        return tele;
    }
}
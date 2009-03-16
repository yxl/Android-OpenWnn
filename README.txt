-------------------------------------------------------------------------------
                            OpenWnn for Android README

                          Internal Version (2009.3.5-E/J)
                  
     (C) Copyright OMRON SOFTWARE Co., Ltd. 2008,2009 All Rights Reserved.
-------------------------------------------------------------------------------

1. About OpenWnn for Android

    OpenWnn for Android is a IME(Input Method Editor) package which
    works on Android's IMF(Input Method Framework).  This version
    contains Japanese IME and English IME.

2. Contents

    This package includes the following items.

    o Document
        . Apache license paper              (text)
        . This README                       (text)
        . Change history                    (text)
        . Java docs of the IME              (HTML)

    o Building environment
        . Building control file		    (XML, makefile, shell script)
        . IME native library source code    (C language)
        . IME resource                      (XML, PNG)
        . IME source code                   (Java)

3. File constitution 

    NOTICE                                                          Apache license paper
    README.txt                                                      This README
    ChangeLog.txt                                                   Change history

    doc/
        *.html                                                      Java docs of the IME

    src/
        IME/
            Android.mk                                              Building control file
            AndroidManifest.xml                                     |

            libs/                                                   IME native library source code (C language)
                Android.mk                                          |
                libwnnDictionary/                                   |
                    Android.mk                                      |
                    *.c                                             |
                    *.h                                             |
                    engine/                                         |
                        *.c                                         |
                    include/                                        |
                        *.h                                         |
                libwnnEngDic/                                       |
                    Android.mk                                      |
                    *.c                                             |
                libwnnJpnDic/                                       |
                    Android.mk                                      |
                    *.c                                             |

            res/                                                    IME resource (XML, PNG)
		drawable/                                           |
		    *.xml                                           |
		    *.png                                           |
		layout/                                             |
		    *.xml                                           |
		raw/                                                |
                    type.ogg                                        |
		values/                                             |
		    *.xml                                           |
		values-ja/                                          |
		    *.xml                                           |
		xml/                                                |
		    *.xml                                           |

            src/                                                    IME source code (Java)
                jp/                                                 |
                    co/                                             |
                        omronsoft/                                  |
                            openwnn/                                |
                                *.java                              |
                                EN/                                 |
				    *.java                          |
                                JAJP/                               |
				    *.java                          |

-------------------------------------------------------------------------------

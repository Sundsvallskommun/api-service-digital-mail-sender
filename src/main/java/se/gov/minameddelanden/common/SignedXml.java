package se.gov.minameddelanden.common;

import javax.xml.crypto.dsig.XMLSignature;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class SignedXml extends Xml {

	private final XMLSignature signature;

	SignedXml(final byte[] bytes, final XMLSignature signature) {
		super(bytes);

		this.signature = signature;
	}

	public XMLSignature getSignature() {
		return signature;
	}
}

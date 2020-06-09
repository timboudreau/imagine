/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi.graphics;

/**
 *
 * @author Tim Boudreau
 */
public interface TextDetails extends TextItem {

    int documentOffset();

    int line();

    int charPositionInLine();

    public int documentOffset(float x, float y);

    public int charPositionInLine(float x, float y);

    public int documentOffset(int charOffset);

    public int charPositionInLine(int charOffset);

    public float charXStart(int charOffset);

    public float charXEnd(int charOffset);

    int charOffsetAt(float x, float y);
}

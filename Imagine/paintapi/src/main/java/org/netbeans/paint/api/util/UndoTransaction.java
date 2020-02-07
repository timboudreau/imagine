/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.util;

/**
 *
 * @author Tim Boudreau
 */
public interface UndoTransaction {

    public void commit();

    public void rollback();
}

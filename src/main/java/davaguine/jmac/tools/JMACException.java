/*
 *  21.04.2004 Original verion. davagin@udm.ru.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package davaguine.jmac.tools;

import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class JMACException extends RuntimeException {

    private static final Logger logger = getLogger(JMACException.class.getName());

    private Throwable exception;

    public JMACException() {
    }

    public JMACException(String msg) {
        super(msg);
    }

    public JMACException(String msg, Throwable t) {
        super(msg);
        exception = t;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        if (this.exception == null) {
            super.printStackTrace(ps);
        } else {
            logger.log(Level.ERROR, exception.getMessage(), exception);
        }
    }
}

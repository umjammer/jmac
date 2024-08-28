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

package davaguine.jmac.player;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class FactoryRegistry extends AudioDeviceFactory {

    static private FactoryRegistry instance = null;

    static synchronized public FactoryRegistry systemRegistry() {
        if (instance == null) {
            instance = new FactoryRegistry();
            instance.registerDefaultFactories();
        }
        return instance;
    }

    protected final Map<Class<?>, AudioDeviceFactory> factories = new HashMap<>();

    /**
     * Registers an <code>AudioDeviceFactory</code> instance
     * with this registry.
     */
    public void addFactory(AudioDeviceFactory factory) {
        factories.put(factory.getClass(), factory);
    }

    public void removeFactoryType(Class<?> cls) {
        factories.remove(cls);
    }

    public void removeFactory(AudioDeviceFactory factory) {
        factories.remove(factory.getClass());
    }

    @Override
    public AudioDevice createAudioDevice() {
        AudioDevice device = null;
        AudioDeviceFactory[] factories = getFactoriesPriority();

        if (factories == null)
            throw new JMACPlayerException(this + ": no factories registered");

        JMACPlayerException lastEx = null;
        for (int i = 0; (device == null) && (i < factories.length); i++) {
            try {
                device = factories[i].createAudioDevice();
            } catch (JMACPlayerException ex) {
                lastEx = ex;
            }
        }

        if (device == null && lastEx != null)
            throw new JMACPlayerException("Cannot create AudioDevice", lastEx);

        return device;
    }


    protected AudioDeviceFactory[] getFactoriesPriority() {
        AudioDeviceFactory[] fa = null;
        synchronized (factories) {
            int size = factories.size();
            if (size != 0) {
                fa = factories.values().toArray(AudioDeviceFactory[]::new);
            }
        }
        return fa;
    }

    protected void registerDefaultFactories() {
        addFactory(new JavaSoundAudioDeviceFactory());
    }
}

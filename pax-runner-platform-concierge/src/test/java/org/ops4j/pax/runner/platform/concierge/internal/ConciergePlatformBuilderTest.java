/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.runner.platform.concierge.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static org.easymock.EasyMock.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.runner.platform.BundleReference;
import org.ops4j.pax.runner.platform.Configuration;
import org.ops4j.pax.runner.platform.LocalBundle;
import org.ops4j.pax.runner.platform.PlatformContext;
import org.ops4j.pax.runner.platform.PlatformException;
import org.ops4j.pax.runner.platform.internal.PlatformContextImpl;

public class ConciergePlatformBuilderTest
{

    private File m_workDir;
    private BundleContext m_bundleContext;
    private Configuration m_configuration;
    private PlatformContext m_platformContext;

    @Before
    public void setUp()
        throws IOException
    {
        m_bundleContext = createMock( BundleContext.class );
        m_configuration = createMock( Configuration.class );
        m_workDir = File.createTempFile( "runner", "" );
        m_workDir.delete();
        m_workDir = new File( m_workDir.getAbsolutePath() );
        m_workDir.mkdirs();
        m_workDir.deleteOnExit();
        m_platformContext = new PlatformContextImpl();
        m_platformContext.setConfiguration( m_configuration );
        m_platformContext.setWorkingDirectory( m_workDir );
    }

    @After
    public void tearDown()
    {
        FileUtils.delete( m_workDir );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundleContext()
    {
        new ConciergePlatformBuilder( null, "version" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullVersion()
    {
        new ConciergePlatformBuilder( m_bundleContext, null );
    }

    @Test
    public void mainClassName()
    {
        replay( m_bundleContext );
        assertEquals(
            "Main class name",
            "ch.ethz.iks.concierge.framework.Framework",
            new ConciergePlatformBuilder( m_bundleContext, "version" ).getMainClassName()
        );
        verify( m_bundleContext );
    }

    @Test
    public void getDefinition_1_0_0()
        throws IOException
    {
        Bundle bundle = createMock( Bundle.class );

        expect( m_bundleContext.getBundle() ).andReturn( bundle );
        expect( bundle.getResource( "META-INF/platform-concierge/definition-1.0.0.xml" ) ).andReturn(
            FileUtils.getFileFromClasspath( "META-INF/platform-concierge/definition-1.0.0.xml" ).toURL()
        );

        replay( m_bundleContext, bundle );
        assertNotNull(
            "Definition input stream",
            new ConciergePlatformBuilder( m_bundleContext, "1.0.0" ).getDefinition()
        );
        verify( m_bundleContext, bundle );
    }

    @Test
    public void getRequiredProfilesWithoutConsole()
    {
        expect( m_configuration.startConsole() ).andReturn( null );

        replay( m_bundleContext, m_configuration );
        assertNull(
            "Required profiles is not null",
            new ConciergePlatformBuilder( m_bundleContext, "version" ).getRequiredProfile( m_platformContext )
        );
        verify( m_bundleContext, m_configuration );
    }

    @Test
    public void getRequiredProfilesWithConsole()
    {
        expect( m_configuration.startConsole() ).andReturn( true );

        replay( m_bundleContext, m_configuration );
        assertEquals(
            "Required profiles",
            "tui",
            new ConciergePlatformBuilder( m_bundleContext, "version" ).getRequiredProfile( m_platformContext )
        );
        verify( m_bundleContext, m_configuration );
    }

    @Test
    public void getArguments()
        throws MalformedURLException
    {
        replay( m_bundleContext );
        assertArrayEquals(
            "Arguments",
            null,
            new ConciergePlatformBuilder( m_bundleContext, "version" ).getArguments( m_platformContext )
        );
        verify( m_bundleContext );
    }

    @Test
    public void getVMOptions()
    {
        expect( m_configuration.getBootDelegation() ).andReturn( "javax.*" );

        replay( m_configuration, m_bundleContext );
        assertArrayEquals(
            "System options",
            new String[]{
                "-Dosgi.maxLevel=100",
                "-Dxargs=" +
                m_platformContext.normalizeAsPath( new File( m_workDir, "concierge/config.ini" ) ),
                "-D" + Constants.FRAMEWORK_BOOTDELEGATION + "=javax.*"
            },
            new ConciergePlatformBuilder( m_bundleContext, "version" ).getVMOptions( m_platformContext )
        );
        verify( m_configuration, m_bundleContext );
    }

    @Test
    public void getVMOptionsWithoutBootDelegation()
    {
        expect( m_configuration.getBootDelegation() ).andReturn( null );

        replay( m_configuration, m_bundleContext );
        assertArrayEquals(
            "System options",
            new String[]{
                "-Dosgi.maxLevel=100",
                "-Dxargs=" +
                m_platformContext.normalizeAsPath( new File( m_workDir, "concierge/config.ini" ) )

            },
            new ConciergePlatformBuilder( m_bundleContext, "version" ).getVMOptions( m_platformContext )
        );
        verify( m_configuration, m_bundleContext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void getSystemPropertiesWithNullPlatformContext()
    {
        replay( m_bundleContext );
        new ConciergePlatformBuilder( m_bundleContext, "version" ).getVMOptions( null );
        verify( m_bundleContext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void prepareWithNullPlatformContext()
        throws PlatformException
    {
        replay( m_bundleContext );
        new ConciergePlatformBuilder( m_bundleContext, "version" ).prepare( null );
        verify( m_bundleContext );
    }

    // tests that the platform configuration ini file is correct with no bundles.
    // also tests that the start level is not set if is not configured
    // also tests that the default start level is not set if is not configured
    @Test
    public void prepareWithoutBundles()
        throws PlatformException, IOException
    {
        m_platformContext.setSystemPackages( "sys.package.one,sys.package.two" );
        Properties properties = new Properties();
        properties.setProperty( "myProperty", "myValue" );
        m_platformContext.setProperties( properties );

        expect( m_configuration.usePersistedState() ).andReturn( false );
        expect( m_configuration.getStartLevel() ).andReturn( null );
        expect( m_configuration.getBundleStartLevel() ).andReturn( null );

        replay( m_bundleContext, m_configuration );
        new ConciergePlatformBuilder( m_bundleContext, "version" ).prepare( m_platformContext );
        verify( m_bundleContext, m_configuration );

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(
            "${basedir.path}",
            m_platformContext.normalizeAsPath( new File( m_workDir, "concierge" ) )
        );

        compareFiles(
            FileUtils.getFileFromClasspath( "conciergeplatformbuilder/configWithNoBundles.ini" ),
            new File( m_workDir + "/concierge/config.ini" ),
            true,
            replacements
        );
    }

    // tests that the platform configuration ini file is correct with bundles to be installed.
    // also tests that the start level is set correctly if configured
    // also tests that the default start level is set correctly if configured
    @Test
    public void prepare()
        throws PlatformException, IOException
    {
        List<LocalBundle> bundles = new ArrayList<LocalBundle>();

        // a bunlde with start level that should start
        LocalBundle bundle1 = createMock( LocalBundle.class );
        bundles.add( bundle1 );
        BundleReference reference1 = createMock( BundleReference.class );
        expect( bundle1.getFile() ).andReturn( new File( m_workDir, "bundles/bundle1.jar" ) );
        expect( bundle1.getBundleReference() ).andReturn( reference1 );
        expect( reference1.getStartLevel() ).andReturn( 10 );
        expect( reference1.shouldStart() ).andReturn( true );

        // a bundle with only start level that should not start
        LocalBundle bundle2 = createMock( LocalBundle.class );
        bundles.add( bundle2 );
        BundleReference reference2 = createMock( BundleReference.class );
        expect( bundle2.getFile() ).andReturn( new File( m_workDir, "bundles/bundle2.jar" ) );
        expect( bundle2.getBundleReference() ).andReturn( reference2 );
        expect( reference2.getStartLevel() ).andReturn( 10 );
        expect( reference2.shouldStart() ).andReturn( null );

        // a bunlde without start level that should start
        LocalBundle bundle3 = createMock( LocalBundle.class );
        bundles.add( bundle3 );
        BundleReference reference3 = createMock( BundleReference.class );
        expect( bundle3.getFile() ).andReturn( new File( m_workDir, "bundles/bundle3.jar" ) );
        expect( bundle3.getBundleReference() ).andReturn( reference3 );
        expect( reference3.getStartLevel() ).andReturn( null );
        expect( reference3.shouldStart() ).andReturn( true );

        // a bundle without start level that should not start
        LocalBundle bundle4 = createMock( LocalBundle.class );
        bundles.add( bundle4 );
        BundleReference reference4 = createMock( BundleReference.class );
        expect( bundle4.getFile() ).andReturn( new File( m_workDir, "bundles/bundle4.jar" ) );
        expect( bundle4.getBundleReference() ).andReturn( reference4 );
        expect( reference4.getStartLevel() ).andReturn( null );
        expect( reference4.shouldStart() ).andReturn( null );

        expect( m_configuration.usePersistedState() ).andReturn( true );
        expect( m_configuration.getStartLevel() ).andReturn( 10 );
        expect( m_configuration.getBundleStartLevel() ).andReturn( 20 );

        m_platformContext.setBundles( bundles );
        m_platformContext.setSystemPackages( "sys.package.one,sys.package.two" );
        Properties properties = new Properties();
        properties.setProperty( "myProperty", "myValue" );
        m_platformContext.setProperties( properties );

        replay( m_bundleContext, m_configuration,
                bundle1, bundle2, bundle3,
                reference1, reference2, reference3, bundle4, reference4
        );
        new ConciergePlatformBuilder( m_bundleContext, "version" ).prepare( m_platformContext );
        verify( m_bundleContext, m_configuration,
                bundle1, bundle2, bundle3,
                reference1, reference2, reference3, bundle4, reference4
        );

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put(
            "${basedir.path}",
            m_platformContext.normalizeAsPath( new File( m_workDir, "concierge" ) )
        );
        replacements.put(
            "${bundle1.path}",
            m_platformContext.normalizeAsUrl( new File( m_workDir, "bundles/bundle1.jar" ) )
        );
        replacements.put(
            "${bundle2.path}",
            m_platformContext.normalizeAsUrl( new File( m_workDir, "bundles/bundle2.jar" ) )
        );
        replacements.put(
            "${bundle3.path}",
            m_platformContext.normalizeAsUrl( new File( m_workDir, "bundles/bundle3.jar" ) )
        );
        replacements.put(
            "${bundle4.path}",
            m_platformContext.normalizeAsUrl( new File( m_workDir, "bundles/bundle4.jar" ) )
        );

        compareFiles(
            FileUtils.getFileFromClasspath( "conciergeplatformbuilder/config.ini" ),
            new File( m_workDir + "/concierge/config.ini" ),
            true,
            replacements
        );
    }

    private static void compareFiles( File expected, File actual, boolean reverse, Map<String, String> replacements )
        throws IOException
    {
        BufferedReader expectedReader = null;
        BufferedReader actualReader = null;
        try
        {
            expectedReader = new BufferedReader( new FileReader( expected ) );
            actualReader = new BufferedReader( new FileReader( actual ) );
            String actualLine, expectedLine;
            int lineNumber = 1;
            while( ( actualLine = actualReader.readLine() ) != null )
            {
                expectedLine = expectedReader.readLine();
                if( reverse )
                {
                    if( replacements != null )
                    {
                        for( Map.Entry<String, String> entry : replacements.entrySet() )
                        {
                            expectedLine = expectedLine.replace( entry.getKey(), entry.getValue() );
                        }
                    }
                    assertEquals( "Config ini line " + lineNumber++, expectedLine, actualLine );
                }
                else
                {
                    if( replacements != null )
                    {
                        for( Map.Entry<String, String> entry : replacements.entrySet() )
                        {
                            actualLine = actualLine.replace( entry.getKey(), entry.getValue() );
                        }
                    }
                    assertEquals( "Config ini line " + lineNumber++, actualLine, expectedLine );
                }
            }
        }
        finally
        {
            if( expectedReader != null )
            {
                expectedReader.close();
            }
            if( actualReader != null )
            {
                actualReader.close();
            }
        }
        if( reverse )
        {
            compareFiles( actual, expected, false, replacements );
        }
    }


}

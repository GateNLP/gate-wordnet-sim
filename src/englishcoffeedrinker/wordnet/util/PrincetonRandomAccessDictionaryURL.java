/*
 * PrincetonRandomAccessDictionaryURL.java
 * 
 * Copyright (c) 2006-2012, Mark A. Greenwood
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package englishcoffeedrinker.wordnet.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.didion.jwnl.JWNLRuntimeException;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.file.DictionaryFile;
import net.didion.jwnl.dictionary.file.DictionaryFileType;
import net.didion.jwnl.princeton.file.AbstractPrincetonRandomAccessDictionaryFile;

import com.imagero.uio.buffer.HTTPBuffer;

/**
 * A <code>RandomAccessDictionaryFile</code> that accesses URLs named with
 * Princeton's dictionary file naming convention. This is much slower than
 * using a normal File based approach but has the advantage that it allows
 * JWNL to be used within a Web Start app without requiring access to the
 * local file system and hence doesn't require raised access permissions.
 * @author Mark A. Greenwood
 */
public class PrincetonRandomAccessDictionaryURL extends AbstractPrincetonRandomAccessDictionaryFile
{
	/**
	 * Default buffer size to speed up reading from the URL
	 */
	private static final int BUFFER_SIZE = 1024;
	
	/**
	 * The base URL against which is used to resolve each WordNet file.
	 * This stores the URL specified in the XML properties file used
	 * to initialise the JWNL library.
	 */
	private URL _base_url = null;
	
	/**
	 * The full URL to the dictionary file this class represents.
	 */
	private URL _file_url = null;
	
	/**
	 * The length in bytes of the dictionary file (this should probably
	 * be a long but the URLConnection.getContentLength() returns an int
	 * and the HTTPBuffer uses int based offsets to access the URL.
	 */
	private int _file_len = -1;
		
	/**
	 * A buffer used to store a section of the dictionary file so we
	 * don't have to connect to the URL for each byte that is requested.
	 * NOTE: This is set initially to zero length so that we don't try
	 * and read past the end of the file which we might do as this doesn't
	 * seem to be caught by the checks in the read or readLine methods.
	 */
	private byte[] _buffer = new byte[0];
	
	/**
	 * Records the offset within the file at which the buffer starts.
	 */
	private int _buffer_offset = 0;
	
	/**
	 * The current offset within the buffer which holds the current byte,
	 * i.e. that which will be returned by a call to read(). If this is
	 * greater than the length of the buffer it signifies that we need
	 * to connect to the URL again to get a new buffer of data.
	 */
	private int _offset = Integer.MAX_VALUE;
	
	/**
	 * The offset in the file which will be returned on the next call to read().
	 * Note that if _offset is valid (i.e. is within the buffer) then this should
	 * be equal to _buffer_offset + _offset - I suppose we could assert this
	 * somewhere as an extra error check but for now we don't check it.
	 */
	private int _pointer = 0;

	public DictionaryFile newInstance(String path, POS pos, DictionaryFileType fileType)
	{
		return new PrincetonRandomAccessDictionaryURL(path, pos, fileType);
	}

	public PrincetonRandomAccessDictionaryURL() 
	{
		//not sure why this is need as the other constructors are definitely used
		//BUT if this isn't here then the class can't be loaded by JWNL so...
	}

	public PrincetonRandomAccessDictionaryURL(String path, POS pos, DictionaryFileType fileType)
	{
		//Basically pass most of the work off to the super class, but trick it
		//into thinking that the files are in the local directory so that we don't
		//have to supple a path which we don't have
		super("", pos, fileType);
		
		try
		{
			//TODO Either check that the URL is HTTP or improve the class to support other protocols
						
			//create a new URL from the string provided
			_base_url = new URL(path);
		}
		catch (MalformedURLException e)
		{
			//if the URL isn't valid then throw an error
			throw new JWNLRuntimeException("DICTIONARY_EXCEPTION_009", e);
		}
	}
	
	/**
	 * Read from the current position to the end of the line.
	 * @return the contents of the file between the current position
	 *         and the end of the line.
	 */
	public String readLine() throws IOException
	{
        //if the file hasn't been marked as open yet throw an exception
		if (!isOpen()) throw new JWNLRuntimeException("PRINCETON_EXCEPTION_001");
        		
       	//if the URL has been opened then we can continue...
        	
    	//create a StringBuilder to hold the line as we read it
    	StringBuilder line = new StringBuilder();
    	
    	//read in the next character
    	char c = (char)read();
    	        	        	
    	while(c != '\n' && c != '\r')
    	{       		
    		//while we haven't hit the end of the line
    		
    		//append the character to the end of the line read so far
    		line.append(c);
    		
    		//get the next character from the file
    		c = (char)read();
    	}
    	
    	//scoop up any remaining line terminating characters
    	while (c == '\n' || c == '\r') c = (char)read();
    	
    	//to read the full line we had to peek at the next character
    	//in the file, so seek back one space otherwise the next
    	//readLine() will get the line minus the 1st character
    	seek(_pointer-1);
    	
    	//trim off any random white space and return the line
    	return line.toString().trim();
	}
	
	/**
	 * Move to the specified position in the file. This has the effect that
	 * the next call to read() will return the byte at position pos.
	 * @param pos the position to move to in the file.
	 */
	public void seek(long pos)
	{
		//we don't actually do any seeking in the method we just
		//get ready for the next call to read()
		
		//change the pointer to point to the new position
		_pointer = (int)pos;
		
		//work out what the offset of the end of the buffer currently is
		int _buffer_end = _buffer_offset + _buffer.length;
		
		if (_pointer >= _buffer_offset && _pointer < _buffer_end)
		{
			//if the new position we are seeking to is still within
			//the currently loaded buffer then move the offset marker
			//so that we don't have to actually access the URL
			_offset = _pointer - _buffer_offset;
		}
		else
		{		
			//the position we are seeking to is outside the scope of the buffer
			//so invalidate the buffer by setting the offset to well after the
			//end of the current buffer
			_offset = Integer.MAX_VALUE;
		}
	}

	/**
	 * Gets the current position within the file.
	 * @return the current position within the file.
	 */
	public long getFilePointer()
	{
		//simply return the held pointer
		return _pointer;
	}
	
	/**
	 * Allows a calling method to determine if the file is currently open
	 * for reading.
	 * @return returns true if the file is open for reading and false otherwise.
	 */
	public boolean isOpen()
	{
		//the file is classed as open if the fully specified URL
		//for this dictionary file is non-null so...
		
		//check if the URL is non-null
		return (_file_url != null);
	}
		
	/**
	 * Closes the dictionary file.
	 */
	public void close()
	{
		//mark the file as closed by nullifying the URL variable
		_file_url = null;
	}

	/**
	 * Opens the dictionary file represented by resolving the name of the
	 * specified File object against the storred base URL.
	 * @param path the name of the dictionary file to open.
	 */
	protected void openFile(File path) throws IOException
	{
		//get a fully specified URL for this dictionary file by resolving
		//the file name from the path object against the previously
		//storred base URL. 
		_file_url = new URL(_base_url, path.getName());
		
		//get the length of the file and store it for letter.
		_file_len = _file_url.openConnection().getContentLength();
			
		//make sure we are pointing at the beginning of the file
		_pointer = 0;
	}

	/**
	 * Gets the length of the file in bytes.
	 * @return the length of the file in bytes;
	 */
	public long length()
	{
		return _file_len;
	}

	/**
	 * Reads the next byte from the file.
	 * @return the next byte read from the file.
	 * @throws IOException if an error occurs reading from the file.
	 */
	public int read() throws IOException
	{	
		if (_pointer >= _file_len)
		{
			//if the pointer is past the end of the file...
			
			//then reset the pointer to the end of the file and...
			_pointer = _file_len;			
			
			//... return -1 to signify we hit the end of the file
			return -1;
		}
		
		if (_offset >= _buffer.length)
		{
			//if we are trying to read from a position past the end of the
			//current buffer then...
			
			//create a new buffer to read into making sure
			//that it isn't bigger than the distance between the
			//current position in the file and the end of the file
			
			int bufSize = (_file_len-_pointer > BUFFER_SIZE ? BUFFER_SIZE : _file_len-_pointer); 		
			
			_buffer = new byte[bufSize];
			
			//read just the required section of the file using a clever
			//feature of HTTP/1.1 known as byte serving
			HTTPBuffer buf = new HTTPBuffer(_file_url,_pointer,_buffer.length);
			
			//fill the buffer using the data we have just read in
			_buffer = buf.getData();
					
			//remember that the start of the buffer is the current pointer
			_buffer_offset = _pointer;
			
			//set the offset so that the next access of the buffer will
			//be from the beginning
			_offset = 0;
		}
		
		//get the requested byte from the buffer
		int val = _buffer[_offset];
		
		//move the buffer pointer one position further forward
		++_offset;		
		
		//move the publicly visible pointer one position further forward
		++_pointer;
				
		//return the byte the calling method requested
		return val;
	}
}
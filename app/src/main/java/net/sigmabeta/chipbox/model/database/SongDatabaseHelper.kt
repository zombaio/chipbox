package net.sigmabeta.chipbox.model.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.sigmabeta.chipbox.model.objects.Track
import net.sigmabeta.chipbox.util.*
import org.apache.commons.io.FileUtils
import rx.Observable
import java.io.File
import java.util.*

val DB_VERSION = 1
val DB_FILENAME = "songs.db"

val COLUMN_DB_ID = 0

val COLUMN_GAME_PLATFORM = 1
val COLUMN_GAME_TITLE = 2
val COLUMN_GAME_DESCRIPTION = 3
val COLUMN_GAME_COMPANY = 4

val COLUMN_ARTIST_NAME = 1

val COLUMN_FOLDER_PATH = 1

val COLUMN_TRACK_NUMBER = 1
val COLUMN_TRACK_PATH = 2
val COLUMN_TRACK_TITLE = 3
val COLUMN_TRACK_GAME_ID = 4
val COLUMN_TRACK_GAME_TITLE = 5
val COLUMN_TRACK_GAME_PLATFORM = 6
val COLUMN_TRACK_ARTIST_ID = 7
val COLUMN_TRACK_ARTIST = 8
val COLUMN_TRACK_LENGTH = 9
val COLUMN_TRACK_INTRO_LENGTH = 10
val COLUMN_TRACK_LOOP_LENGTH = 11

val KEY_DB_ID = "_id"

val KEY_GAME_PLATFORM = "platform"
val KEY_GAME_TITLE = "title"
val KEY_GAME_DESCRIPTION = "description"
val KEY_GAME_COMPANY = "company"

val KEY_ARTIST_NAME = "name"

val KEY_FOLDER_PATH = "path"

val KEY_TRACK_NUMBER = "number"
val KEY_TRACK_PATH = "path"
val KEY_TRACK_TITLE = "title"
val KEY_TRACK_GAME_ID = "game_id"
val KEY_TRACK_GAME_TITLE = "game_title"
val KEY_TRACK_GAME_PLATFORM = "game_platform"
val KEY_TRACK_ARTIST_ID = "artist_id"
val KEY_TRACK_ARTIST = "artist"
val KEY_TRACK_LENGTH = "length"
val KEY_TRACK_INTRO_LENGTH = "intro_length"
val KEY_TRACK_LOOP_LENGTH = "loop_length"

val TABLE_NAME_FOLDERS = "folders"
val TABLE_NAME_GAMES = "games"
val TABLE_NAME_ARTISTS = "artists"
val TABLE_NAME_TRACKS = "tracks"

private val SQL_TYPE_PRIMARY = "INTEGER PRIMARY KEY"
private val SQL_TYPE_INTEGER = "INTEGER"
private val SQL_TYPE_STRING = "TEXT"

private val SQL_CONSTRAINT_UNIQUE = "UNIQUE"

private val SQL_CREATE = "CREATE TABLE"
private val SQL_DELETE = "DROP TABLE IF EXISTS"
private val SQL_FOREIGN = "FOREIGN KEY"
private val SQL_REFERENCES = "REFERENCES"

private val SQL_CREATE_FOLDERS = "${SQL_CREATE} ${TABLE_NAME_FOLDERS} (${KEY_DB_ID} ${SQL_TYPE_PRIMARY}, " +
        "${KEY_FOLDER_PATH} ${SQL_TYPE_STRING} ${SQL_CONSTRAINT_UNIQUE})"

private val SQL_CREATE_GAMES = "${SQL_CREATE} ${TABLE_NAME_GAMES} (${KEY_DB_ID} ${SQL_TYPE_PRIMARY}, " +
        "${KEY_GAME_PLATFORM} ${SQL_TYPE_INTEGER}, " +
        "${KEY_GAME_TITLE} ${SQL_TYPE_STRING}, " +
        "${KEY_GAME_DESCRIPTION} ${SQL_TYPE_STRING}, " +
        "${KEY_GAME_COMPANY} ${SQL_TYPE_STRING})"

private val SQL_CREATE_ARTISTS = "${SQL_CREATE} ${TABLE_NAME_ARTISTS} (${KEY_DB_ID} ${SQL_TYPE_PRIMARY}, " +
        "${KEY_ARTIST_NAME} ${SQL_TYPE_STRING})"

private val SQL_CREATE_TRACKS = "${SQL_CREATE} ${TABLE_NAME_TRACKS} (${KEY_DB_ID} ${SQL_TYPE_PRIMARY}, " +
        "${KEY_TRACK_NUMBER} ${SQL_TYPE_INTEGER}, " +
        "${KEY_TRACK_PATH} ${SQL_TYPE_STRING}, " +
        "${KEY_TRACK_TITLE} ${SQL_TYPE_STRING}, " +
        "${KEY_TRACK_GAME_ID} ${SQL_TYPE_INTEGER}, " +
        "${KEY_TRACK_GAME_TITLE} ${SQL_TYPE_STRING}, " +
        "${KEY_TRACK_GAME_PLATFORM} ${SQL_TYPE_INTEGER}, " +
        "${KEY_TRACK_ARTIST_ID} ${SQL_TYPE_INTEGER}, " +
        "${KEY_TRACK_ARTIST} ${SQL_TYPE_STRING}, " +
        "${KEY_TRACK_LENGTH} ${SQL_TYPE_INTEGER}, " +
        "${KEY_TRACK_INTRO_LENGTH} ${SQL_TYPE_INTEGER}, " +
        "${KEY_TRACK_LOOP_LENGTH} ${SQL_TYPE_INTEGER}, " +
        "${SQL_FOREIGN}(${KEY_TRACK_GAME_ID}) ${SQL_REFERENCES} ${TABLE_NAME_GAMES}(${KEY_DB_ID}))"

private val SQL_DELETE_GAMES = "${SQL_DELETE} ${TABLE_NAME_GAMES}"
private val SQL_DELETE_ARTIST = "${SQL_DELETE} ${TABLE_NAME_ARTISTS}"
private val SQL_DELETE_TRACKS = "${SQL_DELETE} ${TABLE_NAME_TRACKS}"

class SongDatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DB_FILENAME, null, DB_VERSION) {
    override fun onCreate(database: SQLiteDatabase) {
        logDebug("[SongDatabaseHelper] Creating database...")

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_GAMES)
        database.execSQL(SQL_CREATE_GAMES)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_FOLDERS)
        database.execSQL(SQL_CREATE_FOLDERS)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_TRACKS)
        database.execSQL(SQL_CREATE_TRACKS)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_ARTISTS)
        database.execSQL(SQL_CREATE_ARTISTS)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        logInfo("[SongDatabaseHelper] Upgrading database from schema version " + oldVersion + " to " + newVersion)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_DELETE_GAMES)
        database.execSQL(SQL_DELETE_GAMES)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_DELETE_ARTIST)
        database.execSQL(SQL_DELETE_ARTIST)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_DELETE_TRACKS)
        database.execSQL(SQL_DELETE_TRACKS)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_GAMES)
        database.execSQL(SQL_CREATE_GAMES)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_ARTISTS)
        database.execSQL(SQL_CREATE_ARTISTS)

        logVerbose("[SongDatabaseHelper] Executing SQL: " + SQL_CREATE_TRACKS)
        database.execSQL(SQL_CREATE_TRACKS)

        logVerbose("[SongDatabaseHelper] Re-scanning library with new schema.")
        scanLibrary()
    }

    fun addDirectory(path: String): Boolean {
        val database = writableDatabase

        val values = ContentValues()

        values.put(KEY_FOLDER_PATH, path)

        val id = database.insert(TABLE_NAME_FOLDERS, null, values)
        database.close()

        if (id >= 0) {
            logInfo("[SongDatabaseHelper] Successfully added folder to database.")
            return true
        } else {
            logError("[SongDatabaseHelper] Unable to add folder to database.")
            return false
        }
    }

    fun getTrack(trackId: Long): Observable<Track> {
        return Observable.create(
                {
                    logInfo("[SongDatabaseHelper] Getting track #${trackId}...")

                    val whereClause: String?
                    val whereArgs: Array<String>?

                    // If -1 passed in, return all games. Else, return games for one platform only.
                    if (trackId != -1.toLong()) {
                        whereClause = "${KEY_DB_ID} = ?"
                        whereArgs = arrayOf(trackId.toString())
                    } else {
                        it.onError(Exception("Bad track ID."))
                        return@create
                    }

                    val database = readableDatabase

                    val resultCursor = database.query(
                            TABLE_NAME_TRACKS,
                            null,
                            whereClause,
                            whereArgs,
                            null,
                            null,
                            null
                    )

                    logVerbose("[SongDatabaseHelper] Cursor size: ${resultCursor.count}")

                    if (resultCursor.moveToFirst()) {
                        val track = getTrackFromCursor(resultCursor)

                        it.onNext(track)
                        it.onCompleted()
                    } else {
                        it.onError(Exception("Couldn't find track."))
                    }
                }
        )
    }

    fun getGamesList(platform: Int): Observable<Cursor> {
        return Observable.create(
                {
                    logInfo("[SongDatabaseHelper] Reading games list...")

                    val whereClause: String?
                    val whereArgs: Array<String>?

                    // If -1 passed in, return all games. Else, return games for one platform only.
                    if (platform != Track.PLATFORM_ALL) {
                        whereClause = "${KEY_GAME_PLATFORM} = ?"
                        whereArgs = arrayOf(platform.toString())
                    } else {
                        whereClause = null
                        whereArgs = null
                    }

                    val database = readableDatabase

                    val resultCursor = database.query(
                            TABLE_NAME_GAMES,
                            null,
                            whereClause,
                            whereArgs,
                            null,
                            null,
                            "${KEY_GAME_TITLE} ASC"
                    )

                    logVerbose("[SongDatabaseHelper] Cursor size: ${resultCursor.count}")

                    it.onNext(resultCursor)
                    it.onCompleted()
                }
        )
    }

    fun getArtistList(): Observable<Cursor> {
        return Observable.create(
                {
                    logInfo("[SongDatabaseHelper] Reading artist list...")

                    val database = readableDatabase

                    val resultCursor = database.query(
                            TABLE_NAME_ARTISTS,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "${KEY_ARTIST_NAME} ASC"
                    )

                    logVerbose("[SongDatabaseHelper] Cursor size: ${resultCursor.count}")

                    it.onNext(resultCursor)
                    it.onCompleted()
                }
        )
    }

    fun getSongList(): Observable<Cursor> {
        return Observable.create(
                {
                    logInfo("[SongDatabaseHelper] Reading song list...")

                    val database = readableDatabase

                    val resultCursor = database.query(
                            TABLE_NAME_TRACKS,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "${KEY_TRACK_TITLE} ASC"
                    )

                    logVerbose("[SongDatabaseHelper] Result size: ${resultCursor.count}")

                    it.onNext(resultCursor)
                    it.onCompleted()
                }
        )
    }

    fun getSongListForArtist(artist: Long): Observable<Cursor> {
        return Observable.create(
                {
                    logInfo("[SongDatabaseHelper] Reading song list...")
                    val whereClause = "${KEY_TRACK_ARTIST_ID} = ?"
                    val whereArgs = arrayOf(artist.toString())

                    val database = readableDatabase

                    val resultCursor = database.query(
                            TABLE_NAME_TRACKS,
                            null,
                            whereClause,
                            whereArgs,
                            null,
                            null,
                            "${KEY_TRACK_GAME_ID} ASC"
                    )

                    logVerbose("[SongDatabaseHelper] Result size: ${resultCursor.count}")

                    it.onNext(resultCursor)
                    it.onCompleted()
                }
        )
    }

    fun getSongListForGame(game: Long): Observable<Cursor> {
        return Observable.create(
                {
                    logInfo("[SongDatabaseHelper] Reading song list...")
                    val whereClause = "${KEY_TRACK_GAME_ID} = ?"
                    val whereArgs = arrayOf(game.toString())

                    val database = readableDatabase

                    val resultCursor = database.query(
                            TABLE_NAME_TRACKS,
                            null,
                            whereClause,
                            whereArgs,
                            null,
                            null,
                            "${KEY_TRACK_NUMBER} ASC"
                    )

                    logVerbose("[SongDatabaseHelper] Result size: ${resultCursor.count}")

                    it.onNext(resultCursor)
                    it.onCompleted()
                }
        )
    }

    fun scanLibrary(): Observable<String?> {
        return Observable.create(
                {
                    // OnSubscribe.call. it: String
                    logInfo("[SongDatabaseHelper] Scanning library...")

                    val database = writableDatabase

                    it.onNext("Removing missing files from the library...")
                    database.beginTransaction()
                    // Before scanning known folders, go through the game table and remove any entries for which the file itself is missing.
                    trimMissingFiles(database)

                    database.setTransactionSuccessful()
                    database.endTransaction()

                    // Get a cursor listing all the folders the user has added to the library.
                    val folderCursor = database.query(TABLE_NAME_FOLDERS,
                            null, // Get all columns.
                            null, // Get all rows.
                            null,
                            null, // No grouping.
                            null,
                            null) // Order of folders is irrelevant.

                    // Possibly overly defensive, but ensures that moveToNext() does not skip a row.
                    folderCursor.moveToPosition(-1)

                    // Iterate through all results of the DB query (i.e. all folders in the library.)
                    while (folderCursor.moveToNext()) {
                        val folderPath = folderCursor.getString(COLUMN_FOLDER_PATH)
                        val folder = File(folderPath)

                        it.onNext("Scanning for tracks: ${folderPath}")

                        scanFolder(folder, database)
                    }

                    folderCursor.close()
                    database.close()

                    it.onCompleted()
                }
        )
    }

    private fun scanFolder(folder: File, database: SQLiteDatabase) {
        database.beginTransaction()

        val folderPath = folder.absolutePath
        logInfo("[SongDatabaseHelper] Reading files from library folder: ${folderPath}")

        var folderGameId: Long? = null

        // Iterate through every file in the folder.
        val children = folder.listFiles()

        if (children != null) {
            var trackCount = 1

            for (file in children) {
                if (!file.isHidden) {
                    if (file.isDirectory) {
                        scanFolder(file, database)
                    } else {
                        val filePath = file.absolutePath

                        val extensionStart = filePath.lastIndexOf('.')
                        if (extensionStart > 0) {
                            val fileExtension = filePath.substring(extensionStart)

                            // Check that the file has an extension we care about before trying to read out of it.
                            if (EXTENSIONS_MUSIC.contains(fileExtension)) {
                                val track = readTrackInfoFromPath(filePath, 0)

                                track?.trackNumber = trackCount
                                trackCount += 1

                                if (track != null) {
                                    val values = getContentValuesFromTrack(track, database)

                                    folderGameId = values.getAsLong(KEY_TRACK_GAME_ID)
                                    addTrackToDatabase(values, database)
                                } else {
                                    logError("[SongDatabaseHelper] Couldn't read track at ${filePath}")
                                }
                            } else if (EXTENSIONS_IMAGES.contains(fileExtension)) {
                                if (folderGameId != null) {
                                    copyImageToInternal(folderGameId, file)
                                } else {
                                    logError("[SongDatabaseHelper] Found image, but game ID unknown: ${filePath}")
                                }
                            }
                        }
                    }
                }
            }

            database.setTransactionSuccessful()

        } else if (!folder.exists()) {
            logError("[SongDatabaseHelper] Folder no longer exists: ${folderPath}")
        } else {
            logError("[SongDatabaseHelper] Folder contains no tracks:  ${folderPath}")
        }

        database.endTransaction()
    }

    private fun addTrackToDatabase(values: ContentValues, database: SQLiteDatabase) {
        // Try to update an existing track first.
        val rowsMatched = database.update(TABLE_NAME_TRACKS, // Which table to update.
                values, // The values to fill the row with.
                "${KEY_DB_ID} = ?", // The WHERE clause used to find the right row.
                arrayOf(values.getAsString(KEY_DB_ID)))

        // TODO Does the above call make any sense? How would it know what ID to use?

        // If update fails, insert a new game instead.
        if (rowsMatched == 0) {
            database.insert(TABLE_NAME_TRACKS,
                    null,
                    values)

            logInfo("[SongDatabaseHelper] Added track: " + values.getAsString(KEY_TRACK_TITLE))
        } else {
            logInfo("[SongDatabaseHelper] Updated track: " + values.getAsString(KEY_TRACK_TITLE))
        }
    }

    private fun copyImageToInternal(gameId: Long, sourceFile: File) {
        val storageDir = context.getExternalFilesDir(null)

        val targetDirPath = storageDir.absolutePath + "/images/" + gameId.toString()
        val targetDir = File(targetDirPath)

        targetDir.mkdirs()

        val sourcePath = sourceFile.path
        val extensionStart = sourcePath.lastIndexOf('.')
        val fileExtension = sourcePath.substring(extensionStart)

        val targetFilePath = targetDirPath + "/local" + fileExtension
        val targetFile = File(targetFilePath)

        FileUtils.copyFile(sourceFile, targetFile)

        logInfo("[SongDatabaseHelper] Copied image: ${sourcePath} to ${targetFilePath}")
    }

    private fun trimMissingFiles(database: SQLiteDatabase) {
        val fileCursor = database.query(TABLE_NAME_TRACKS,
                null, // Get all columns.
                null, // Get all rows.
                null,
                null, // No grouping.
                null,
                null) // Order of games is irrelevant.

        // Possibly overly defensive, but ensures that moveToNext() does not skip a row.
        fileCursor.moveToPosition(-1)

        while (fileCursor.moveToNext()) {
            val trackPath = fileCursor.getString(COLUMN_TRACK_PATH)
            val track = File(trackPath)

            if (!track.exists()) {
                logError("[SongDatabaseHelper] Game file no longer exists. Removing from the library: " + trackPath)
                database.delete(TABLE_NAME_TRACKS,
                        "${KEY_DB_ID}  = ?",
                        arrayOf(fileCursor.getLong(COLUMN_DB_ID).toString()))
            }
        }

        fileCursor.close()
    }

    companion object {
        fun getPlaybackQueueFromCursor(cursor: Cursor): ArrayList<Track> {
            val queue = ArrayList<Track>()

            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                queue.add(getTrackFromCursor(cursor))
            }

            return queue
        }

        private fun getTrackFromCursor(cursor: Cursor): Track {
            return Track(
                    cursor.getLong(COLUMN_DB_ID),
                    cursor.getInt(COLUMN_TRACK_NUMBER),
                    cursor.getString(COLUMN_TRACK_PATH),
                    cursor.getString(COLUMN_TRACK_TITLE),
                    cursor.getLong(COLUMN_TRACK_GAME_ID),
                    cursor.getString(COLUMN_TRACK_GAME_TITLE),
                    cursor.getInt(COLUMN_TRACK_GAME_PLATFORM),
                    cursor.getString(COLUMN_TRACK_ARTIST),
                    cursor.getInt(COLUMN_TRACK_LENGTH),
                    cursor.getInt(COLUMN_TRACK_INTRO_LENGTH),
                    cursor.getInt(COLUMN_TRACK_LOOP_LENGTH)
            )
        }

        private fun getContentValuesFromTrack(track: Track, database: SQLiteDatabase): ContentValues {
            val values = ContentValues()

            values.put(KEY_TRACK_NUMBER, track.trackNumber)
            values.put(KEY_TRACK_PATH, track.path)
            values.put(KEY_TRACK_TITLE, track.title)
            values.put(KEY_TRACK_GAME_ID, getGameId(track.gameTitle, track.platform, database))
            values.put(KEY_TRACK_GAME_TITLE, track.gameTitle)
            values.put(KEY_TRACK_GAME_PLATFORM, track.platform)
            values.put(KEY_TRACK_ARTIST_ID, getArtistId(track.artist, database))
            values.put(KEY_TRACK_ARTIST, track.artist)
            values.put(KEY_TRACK_LENGTH, track.trackLength)
            values.put(KEY_TRACK_INTRO_LENGTH, track.introLength)
            values.put(KEY_TRACK_LOOP_LENGTH, track.loopLength)

            return values
        }

        private fun getArtistId(artist: String, database: SQLiteDatabase): Long {
            val resultCursor = database.query(TABLE_NAME_ARTISTS,
                    null, // Get all columns.
                    "${KEY_ARTIST_NAME} = ?", // Get only the artist matching this name.
                    arrayOf(artist), // The name to match.
                    null, // No grouping.
                    null, // No havingBy.
                    null) // Should only be one result, so order is irrelevant.

            when (resultCursor.count) {
                0 -> {
                    resultCursor.close()
                    return addArtistToDatabase(artist, database)
                }
                1 -> logDebug("[SongDatabaseHelper] Found database entry for artist ${artist}.")
                else -> logError("[SongDatabaseHelper] Found multiple database entries with artist ${artist}")
            }

            resultCursor.moveToFirst()
            val id = resultCursor.getLong(COLUMN_DB_ID)

            resultCursor.close()
            return id
        }

        private fun addArtistToDatabase(artist: String, database: SQLiteDatabase): Long {
            val values = ContentValues()

            values.put(KEY_ARTIST_NAME, artist)

            val artistId = database.insert(TABLE_NAME_ARTISTS,
                    null,
                    values)

            if (artistId < 0) {
                // TODO Do more than just report an error.
                logError("[SongDatabaseHelper] Unable to add artist ${artist} to database.")
                throw UnsupportedOperationException("Unable to add artist ${artist} to database.")
            } else {
                logInfo("[SongDatabaseHelper] Added artist #${artistId}: ${artist} to database.")
            }

            return artistId
        }

        private fun getGameId(gameTitle: String, gamePlatform: Int, database: SQLiteDatabase): Long {
            val resultCursor = database.query(TABLE_NAME_GAMES,
                    null, // Get all columns.
                    "${KEY_GAME_TITLE} = ? AND ${KEY_GAME_PLATFORM} = ?", // Get only the game matching this title.
                    arrayOf(gameTitle, gamePlatform.toString()), // The title to match.
                    null, // No grouping.
                    null, // No havingBy.
                    null) // Should only be one result, so order is irrelevant.

            when (resultCursor.count) {
                0 -> {
                    resultCursor.close()
                    return addGameToDatabase(gameTitle, gamePlatform, database)
                }
                1 -> logDebug("[SongDatabaseHelper] Found database entry for game ${gameTitle}.")
                else -> logError("[SongDatabaseHelper] Found multiple database entries with title ${gameTitle}")
            }

            resultCursor.moveToFirst()
            val id = resultCursor.getLong(COLUMN_DB_ID)

            resultCursor.close()
            return id
        }

        private fun addGameToDatabase(gameTitle: String, gamePlatform: Int, database: SQLiteDatabase): Long {
            val values = ContentValues()

            values.put(KEY_GAME_TITLE, gameTitle)
            values.put(KEY_GAME_PLATFORM, gamePlatform)

            val gameId = database.insert(TABLE_NAME_GAMES,
                    null,
                    values)

            if (gameId < 0) {
                // TODO Do more than just report an error.
                logError("[SongDatabaseHelper] Unable to add game ${gameTitle} to database.")
                throw UnsupportedOperationException("Unable to add game ${gameTitle} to database.")
            } else {
                logInfo("[SongDatabaseHelper] Added game #${gameId}: ${gameTitle} to database.")
            }

            return gameId
        }
    }
}

package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class GitHubTest extends GroovyTestCase {
    def testChangelog =
            '''
## [Unreleased]

## [v1.0.0]
### Added
- everything
    
'''

    @Test
    void testPushGitHubPagesBranch() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat output", "")
        Git git = new Git(scriptMock)
        GitHub github = new GitHub(scriptMock, git)
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")
        scriptMock.expectedShRetValueForScript.put('git remote get-url origin', "https://repo.url")

        github.pushGitHubPagesBranch('website', 'Deploys new version of website')

        assertGitHubPagesBranchToSubFolder('.', scriptMock)
    }

    @Test
    void testPushGitHubPagesBranchToSubFolder() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat output", "")
        Git git = new Git(scriptMock)
        GitHub github = new GitHub(scriptMock, git)
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")
        scriptMock.expectedShRetValueForScript.put('git remote get-url origin', "https://repo.url")

        github.pushGitHubPagesBranch('website', 'Deploys new version of website', 'some-folder')

        assertGitHubPagesBranchToSubFolder('some-folder', scriptMock)
    }

    private void assertGitHubPagesBranchToSubFolder(String subFolder, ScriptMock scriptMock) {
        assert scriptMock.actualGitArgs.url == "https://repo.url"
        assert scriptMock.actualGitArgs.branch == "gh-pages"

        assert scriptMock.actualDir == '.gh-pages'
        assert scriptMock.allActualArgs.contains("cp -rf ../website/* ${subFolder}".toString())
        assert scriptMock.allActualArgs.contains("mkdir -p ${subFolder}".toString())
        assert scriptMock.allActualArgs.contains('git add .')
        assert scriptMock.allActualArgs.contains('git commit -m "Deploys new version of website"')
        assert scriptMock.actualWithEnv.contains("${'GIT_AUTHOR_NAME=User Name'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_COMMITTER_NAME=User Name'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_AUTHOR_EMAIL=user.name@doma.in'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_COMMITTER_EMAIL=user.name@doma.in'}")
        assert scriptMock.allActualArgs.contains('git push origin gh-pages > output')
        assert scriptMock.allActualArgs.last == 'rm -rf .gh-pages'
    }

    @Test
    void testCreateGithubReleaseByChangelog() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat CHANGELOG.md", testChangelog)
        scriptMock.expectedShRetValueForScript.put("cat output", "")
        scriptMock.expectedShRetValueForScript.put("git remote get-url origin", "myRepoName")
        Git git = new Git(scriptMock, "credentials")
        GitHub github = new GitHub(scriptMock, git)
        Changelog changelog = new Changelog(scriptMock)

        github.createGithubReleaseByChangelog("v1.0.0", changelog)

        assertEquals(6, scriptMock.allActualArgs.size())
        int i = 0;
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[i++])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[i++])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[i++])
        assertEquals("cat CHANGELOG.md", scriptMock.allActualArgs[i++])
        assertEquals("git remote get-url origin", scriptMock.allActualArgs[i++])

        String expectedData = "--data '{\"tag_name\": \"v1.0.0\", \"target_commitish\": \"master\", " +
                "\"name\": \"v1.0.0\", \"body\":\"### Added\\n- everything\"}'"
        String expectedHeader = "--header \"Content-Type: application/json\" https://api.github.com/repos/myRepoName/releases"

        assertEquals("curl -u \$GIT_AUTH_USR:\$GIT_AUTH_PSW --request POST ${expectedData} ${expectedHeader}", scriptMock.allActualArgs[i++])
    }
}
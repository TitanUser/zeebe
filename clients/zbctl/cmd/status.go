// Copyright © 2018 Camunda Services GmbH (info@camunda.com)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package cmd

import (
	"github.com/zeebe-io/zeebe/clients/zbctl/utils"

	"github.com/spf13/cobra"
	"github.com/zeebe-io/zeebe/clients/go"
)

// deployWorkflowCmd implements cobra command for cli
var statusCmd = &cobra.Command{
	Use:   "status",
	Short: "Checks the current health of the cluster",
	Long:  ``,
	PreRun: func(cmd *cobra.Command, args []string) {
		initBroker(cmd)
	},
	Run: func(cmd *cobra.Command, args []string) {
		client, err := zbc.NewZBClient(brokerAddr)
		utils.CheckOrExit(err, utils.ExitCodeConfigurationError, defaultErrCtx)

		response, err := client.NewHealthCheckCommand().Send()
		utils.CheckOrExit(err, utils.ExitCodeIOError, defaultErrCtx)

		out.Serialize(response).Flush()
	},
}

func init() {
	rootCmd.AddCommand(statusCmd)
}
